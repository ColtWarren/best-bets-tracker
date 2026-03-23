package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.model.enums.BetType;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import com.coltwarren.best_bets_tracker.repository.DailySnapshotRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Captures AI-generated best bet recommendations from the main
 * sports-betting-analytics app by calling its /api/best-bets/all-sports endpoint.
 *
 * This is the primary data ingestion point for the tracker. It should run
 * once daily (via scheduled job or manual trigger) to snapshot that day's picks.
 */
@Service
public class PredictionCaptureService {

    private static final Logger log = LoggerFactory.getLogger(PredictionCaptureService.class);

    private final WebClient webClient;
    private final PredictionRepository predictionRepository;
    private final DailySnapshotRepository snapshotRepository;

    public PredictionCaptureService(
            @Value("${main-app.base-url}") String mainAppBaseUrl,
            PredictionRepository predictionRepository,
            DailySnapshotRepository snapshotRepository) {
        this.webClient = WebClient.builder()
                .baseUrl(mainAppBaseUrl)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.predictionRepository = predictionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Captures today's best bets from the main app.
     * Creates a DailySnapshot and individual Prediction records for each bet.
     *
     * If today's snapshot already exists, it will be updated with any new picks
     * that weren't previously captured (idempotent by homeTeam + awayTeam + eventStartTime).
     *
     * @return the DailySnapshot for today with all captured predictions
     */
    @Transactional
    public DailySnapshot captureToday() {
        return captureForDate(LocalDate.now());
    }

    /**
     * Captures best bets for a specific date. Useful for backfilling missed days.
     * The main app always returns current recommendations, so this is most accurate
     * when called on the actual day.
     *
     * @param date the date to associate with this snapshot
     * @return the DailySnapshot with captured predictions
     */
    @Transactional
    public DailySnapshot captureForDate(LocalDate date) {
        log.info("Capturing best bets for date: {}", date);

        // Fetch raw best bets JSON from the main app
        Map<String, Object> response = fetchBestBetsFromMainApp();
        if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
            log.error("Failed to fetch best bets from main app. Response: {}", response);
            throw new RuntimeException("Failed to fetch best bets from main app");
        }

        // Get or create today's snapshot
        DailySnapshot snapshot = snapshotRepository.findBySnapshotDate(date)
                .orElseGet(() -> {
                    DailySnapshot newSnapshot = new DailySnapshot();
                    newSnapshot.setSnapshotDate(date);
                    newSnapshot.setTotalPicks(0);
                    newSnapshot.setPicksResolved(0);
                    newSnapshot.setWins(0);
                    newSnapshot.setLosses(0);
                    newSnapshot.setPushes(0);
                    newSnapshot.setNetProfitUnits(BigDecimal.ZERO);
                    return newSnapshot;
                });

        // Parse each bet from the response and create Prediction entities
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> bets = (List<Map<String, Object>>) response.get("bets");
        if (bets == null || bets.isEmpty()) {
            log.warn("No bets returned from main app for date: {}", date);
            snapshot.setTotalPicks(0);
            return snapshotRepository.save(snapshot);
        }

        // Get existing predictions for this snapshot to avoid duplicates
        List<Prediction> existingPredictions = snapshot.getId() != null
                ? predictionRepository.findBySnapshotId(snapshot.getId())
                : List.of();

        List<Prediction> newPredictions = new ArrayList<>();
        int capturedCount = 0;

        for (Map<String, Object> bet : bets) {
            try {
                // Skip if we already captured this exact matchup for this snapshot
                String homeTeam = getStringValue(bet, "homeTeam");
                String awayTeam = getStringValue(bet, "awayTeam");
                String selection = getStringValue(bet, "recommendation");

                if (isDuplicate(existingPredictions, homeTeam, awayTeam, selection)) {
                    log.debug("Skipping duplicate: {} @ {} - {}", awayTeam, homeTeam, selection);
                    continue;
                }

                Prediction prediction = mapBetToPrediction(bet, snapshot);
                newPredictions.add(prediction);
                capturedCount++;
            } catch (Exception e) {
                log.warn("Failed to parse bet: {}. Error: {}", bet, e.getMessage());
            }
        }

        // Save snapshot first (so it has an ID for the FK), then save predictions
        snapshot.setTotalPicks(snapshot.getTotalPicks() + capturedCount);
        snapshot = snapshotRepository.save(snapshot);

        if (!newPredictions.isEmpty()) {
            predictionRepository.saveAll(newPredictions);
        }

        log.info("Captured {} new predictions for {} (total: {})",
                capturedCount, date, snapshot.getTotalPicks());

        return snapshot;
    }

    /**
     * Calls GET /api/best-bets/all-sports on the main app.
     *
     * Expected response structure:
     * {
     *   "success": true,
     *   "bets": [ { sport, homeTeam, awayTeam, gameTime, recommendation,
     *               confidence, analysis, kellyPercent, bestOdds, ... } ]
     * }
     *
     * @return parsed JSON response as a Map, or null on failure
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchBestBetsFromMainApp() {
        try {
            log.debug("Fetching best bets from main app...");
            return webClient.get()
                    .uri("/api/best-bets/all-sports")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("Error fetching best bets from main app: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Maps a raw best bet JSON object from the main app into a Prediction entity.
     * Handles both US sports (2-way odds) and soccer (3-way odds) formats.
     *
     * @param bet      the raw JSON map from the main app response
     * @param snapshot the DailySnapshot this prediction belongs to
     * @return a new Prediction entity (not yet persisted)
     */
    private Prediction mapBetToPrediction(Map<String, Object> bet, DailySnapshot snapshot) {
        Prediction p = new Prediction();
        p.setSnapshot(snapshot);

        // Game info
        p.setSport(parseSport(getStringValue(bet, "sport")));
        p.setHomeTeam(getStringValue(bet, "homeTeam"));
        p.setAwayTeam(getStringValue(bet, "awayTeam"));
        p.setEventName(p.getAwayTeam() + " @ " + p.getHomeTeam());
        p.setEventStartTime(parseGameTime(getStringValue(bet, "gameTime")));

        // AI recommendation
        p.setSelection(getStringValue(bet, "recommendation"));
        p.setConfidence(getBigDecimalValue(bet, "confidence"));
        p.setKellyPercent(getBigDecimalValue(bet, "kellyPercent"));
        p.setAiReasoning(getStringValue(bet, "analysis"));

        // Determine bet type from the recommendation text
        p.setBetType(inferBetType(p.getSelection(), p.getSport()));

        // Extract best odds and sportsbook from the nested bestOdds object
        extractOddsInfo(bet, p);

        // Calculate expected value if not provided
        BigDecimal ev = getBigDecimalValue(bet, "expectedValue");
        p.setExpectedValue(ev);

        // Default status
        p.setStatus(BetResult.PENDING);
        p.setPredictionTime(LocalDateTime.now());

        return p;
    }

    /**
     * Extracts odds and sportsbook info from the bestOdds nested object.
     * Handles both US sports format (homeML, awayML, spreads) and
     * soccer format (homeWinOdds, drawOdds, awayWinOdds).
     */
    @SuppressWarnings("unchecked")
    private void extractOddsInfo(Map<String, Object> bet, Prediction p) {
        Map<String, Object> bestOdds = (Map<String, Object>) bet.get("bestOdds");
        if (bestOdds == null) {
            // Fallback: no odds data available
            p.setOdds(BigDecimal.ZERO);
            p.setSportsbook("Unknown");
            return;
        }

        // Soccer 3-way format
        if (p.getSport() == Sport.SOCCER) {
            extractSoccerOdds(bestOdds, p);
            return;
        }

        // US sports — pick the odds that match the recommendation
        String selection = p.getSelection().toLowerCase();
        if (selection.contains("over") || selection.contains("under")) {
            // Totals bet
            BigDecimal odds = selection.contains("over")
                    ? getBigDecimalFromMap(bestOdds, "overOdds")
                    : getBigDecimalFromMap(bestOdds, "underOdds");
            String book = selection.contains("over")
                    ? getStringFromMap(bestOdds, "overBook")
                    : getStringFromMap(bestOdds, "underBook");
            p.setOdds(odds != null ? odds : BigDecimal.ZERO);
            p.setSportsbook(book != null ? book : "Unknown");
        } else if (selection.contains("+") || selection.contains("-")) {
            // Spread bet — figure out if it's home or away spread
            boolean isHomeSpread = isHomeTeamSelection(selection, p.getHomeTeam());
            BigDecimal odds = isHomeSpread
                    ? getBigDecimalFromMap(bestOdds, "homeSpreadOdds")
                    : getBigDecimalFromMap(bestOdds, "awaySpreadOdds");
            String book = isHomeSpread
                    ? getStringFromMap(bestOdds, "homeSpreadBook")
                    : getStringFromMap(bestOdds, "awaySpreadBook");
            p.setOdds(odds != null ? odds : BigDecimal.ZERO);
            p.setSportsbook(book != null ? book : "Unknown");
        } else {
            // Moneyline — figure out home or away
            boolean isHomePick = isHomeTeamSelection(selection, p.getHomeTeam());
            BigDecimal odds = isHomePick
                    ? getBigDecimalFromMap(bestOdds, "homeML")
                    : getBigDecimalFromMap(bestOdds, "awayML");
            String book = isHomePick
                    ? getStringFromMap(bestOdds, "homeMLBook")
                    : getStringFromMap(bestOdds, "awayMLBook");
            p.setOdds(odds != null ? odds : BigDecimal.ZERO);
            p.setSportsbook(book != null ? book : "Unknown");
        }
    }

    /**
     * Extracts odds for soccer's 3-way format (Home Win / Draw / Away Win).
     * Uses the "outcome" field to determine which odds to grab.
     */
    private void extractSoccerOdds(Map<String, Object> bestOdds, Prediction p) {
        String outcome = getStringFromMap(bestOdds, "outcome");
        if ("HOME_WIN".equals(outcome)) {
            p.setOdds(getBigDecimalFromMap(bestOdds, "homeWinOdds"));
            p.setSportsbook(getStringFromMap(bestOdds, "homeWinBook"));
        } else if ("DRAW".equals(outcome)) {
            p.setOdds(getBigDecimalFromMap(bestOdds, "drawOdds"));
            p.setSportsbook(getStringFromMap(bestOdds, "drawBook"));
        } else {
            p.setOdds(getBigDecimalFromMap(bestOdds, "awayWinOdds"));
            p.setSportsbook(getStringFromMap(bestOdds, "awayWinBook"));
        }
        if (p.getOdds() == null) p.setOdds(BigDecimal.ZERO);
        if (p.getSportsbook() == null) p.setSportsbook("Unknown");
    }

    /**
     * Infers the BetType from the AI recommendation text.
     * Examples:
     *   "Chiefs -3.5"          → SPREAD
     *   "Over 48.5"            → TOTAL_OVER
     *   "Under 48.5"           → TOTAL_UNDER
     *   "Chiefs ML"            → MONEYLINE
     *   "Arsenal to Win"       → HOME_WIN (soccer)
     *   "Draw"                 → DRAW (soccer)
     */
    private BetType inferBetType(String selection, Sport sport) {
        if (selection == null) return BetType.MONEYLINE;

        String lower = selection.toLowerCase();

        // Totals
        if (lower.startsWith("over") || lower.contains(" over ")) return BetType.TOTAL_OVER;
        if (lower.startsWith("under") || lower.contains(" under ")) return BetType.TOTAL_UNDER;

        // Soccer-specific
        if (sport == Sport.SOCCER) {
            if (lower.contains("draw")) return BetType.DRAW;
            if (lower.contains("away") || lower.contains("to win")) return BetType.AWAY_WIN;
            return BetType.HOME_WIN;
        }

        // Moneyline indicators
        if (lower.contains("ml") || lower.contains("moneyline")) return BetType.MONEYLINE;

        // Spread: contains +/- followed by a number (e.g., "Chiefs -3.5")
        if (lower.matches(".*[+-]\\d+\\.?\\d*.*")) return BetType.SPREAD;

        // Default to moneyline
        return BetType.MONEYLINE;
    }

    /**
     * Checks if a selection string refers to the home team.
     * Compares against the home team name (case-insensitive, partial match).
     */
    private boolean isHomeTeamSelection(String selection, String homeTeam) {
        if (selection == null || homeTeam == null) return true;
        String selLower = selection.toLowerCase();
        // Check if any word from the home team name appears in the selection
        for (String word : homeTeam.toLowerCase().split("\\s+")) {
            if (word.length() > 2 && selLower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a prediction with the same matchup and selection already exists
     * in the given list. Prevents duplicate captures if the service runs twice.
     */
    private boolean isDuplicate(List<Prediction> existing, String homeTeam,
                                String awayTeam, String selection) {
        return existing.stream().anyMatch(p ->
                p.getHomeTeam().equalsIgnoreCase(homeTeam)
                        && p.getAwayTeam().equalsIgnoreCase(awayTeam)
                        && p.getSelection().equalsIgnoreCase(selection));
    }

    // === Parsing Helpers ===

    /**
     * Maps sport string from the main app to our Sport enum.
     * The main app uses uppercase names like "NFL", "NBA", "SOCCER".
     */
    private Sport parseSport(String sport) {
        if (sport == null) return Sport.NFL;
        try {
            return Sport.valueOf(sport.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown sport: '{}', defaulting to NFL", sport);
            return Sport.NFL;
        }
    }

    /**
     * Parses ISO-8601 datetime string from the main app.
     * Falls back to current time if parsing fails.
     */
    private LocalDateTime parseGameTime(String gameTime) {
        if (gameTime == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(gameTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            try {
                // Try without the 'T' separator
                return LocalDateTime.parse(gameTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e2) {
                log.warn("Failed to parse game time: '{}', using current time", gameTime);
                return LocalDateTime.now();
            }
        }
    }

    // === Map Value Extraction Helpers ===
    // These safely extract typed values from the untyped JSON Maps.

    private String getStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return null;
        try {
            return new BigDecimal(val.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getStringFromMap(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private BigDecimal getBigDecimalFromMap(Map<String, Object> map, String key) {
        return getBigDecimalValue(map, key);
    }
}
