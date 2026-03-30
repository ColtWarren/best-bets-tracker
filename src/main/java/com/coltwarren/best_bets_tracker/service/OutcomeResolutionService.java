package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import com.coltwarren.best_bets_tracker.model.Outcome;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.model.enums.BetType;
import com.coltwarren.best_bets_tracker.repository.DailySnapshotRepository;
import com.coltwarren.best_bets_tracker.repository.OutcomeRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves pending predictions by checking ESPN's public scoreboard API
 * for final game scores, then determining if each pick WON, LOST, or PUSHED.
 *
 * Settlement logic:
 * - SPREAD: checks if the picked team covered the point spread
 * - MONEYLINE: checks if the picked team won outright
 * - TOTAL_OVER/UNDER: checks if the combined score went over/under the line
 * - SOCCER (HOME_WIN/DRAW/AWAY_WIN): checks the final match result
 *
 * Only settles games that started 4+ hours ago (enough time for completion).
 */
@Service
public class OutcomeResolutionService {

    private static final Logger log = LoggerFactory.getLogger(OutcomeResolutionService.class);

    /**
     * Regex to extract a point spread or total line from the selection string.
     * Matches patterns like: "Chiefs -3.5", "Over 48.5", "Under 215"
     */
    private static final Pattern LINE_PATTERN = Pattern.compile("[+-]?\\d+\\.?\\d*");

    private final WebClient espnClient;
    private final PredictionRepository predictionRepository;
    private final OutcomeRepository outcomeRepository;
    private final DailySnapshotRepository snapshotRepository;

    public OutcomeResolutionService(
            @Value("${espn.api.url}") String espnBaseUrl,
            PredictionRepository predictionRepository,
            OutcomeRepository outcomeRepository,
            DailySnapshotRepository snapshotRepository) {
        this.espnClient = WebClient.builder()
                .baseUrl(espnBaseUrl)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.predictionRepository = predictionRepository;
        this.outcomeRepository = outcomeRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Resolves all eligible pending predictions.
     * A prediction is eligible if it's PENDING and the game started 4+ hours ago.
     *
     * For each eligible prediction:
     * 1. Fetches the ESPN scoreboard for that sport
     * 2. Finds the matching game by team names
     * 3. Checks if the game is final
     * 4. Determines WON/LOST/PUSH based on the bet type and final score
     * 5. Creates an Outcome record and updates the DailySnapshot tallies
     *
     * @return a summary map with counts of settled, failed, and still-pending
     */
    @Transactional
    public Map<String, Object> resolveAll() {
        // Find all predictions that started 4+ hours ago and are still pending
        LocalDateTime cutoff = LocalDateTime.now().minusHours(4);
        List<Prediction> eligible = predictionRepository.findSettlementEligible(cutoff);

        log.info("Found {} predictions eligible for settlement", eligible.size());

        int settled = 0;
        int failed = 0;
        int stillPending = 0;
        List<String> results = new java.util.ArrayList<>();

        for (Prediction prediction : eligible) {
            try {
                BetResult result = resolveOne(prediction);
                if (result != null && result.isSettled()) {
                    settled++;
                    results.add(prediction.getEventName() + ": " + result);
                } else {
                    // Game not final yet or couldn't find it
                    stillPending++;
                }
            } catch (Exception e) {
                log.warn("Failed to resolve prediction {}: {}", prediction.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("Settlement complete: {} settled, {} failed, {} still pending",
                settled, failed, stillPending);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalEligible", eligible.size());
        summary.put("settled", settled);
        summary.put("failed", failed);
        summary.put("stillPending", stillPending);
        summary.put("results", results);
        return summary;
    }

    /**
     * Attempts to resolve a single prediction by checking ESPN for the final score.
     *
     * @param prediction the pending prediction to resolve
     * @return the BetResult (WON/LOST/PUSH) or null if the game isn't final yet
     */
    @Transactional
    public BetResult resolveOne(Prediction prediction) {
        // Skip if already settled or already has an outcome
        if (prediction.getStatus().isSettled()) {
            return prediction.getStatus();
        }
        if (outcomeRepository.findByPredictionId(prediction.getId()).isPresent()) {
            return prediction.getStatus();
        }

        // Fetch scoreboard from ESPN for this sport
        Map<String, Object> gameData = findGameOnEspn(prediction);
        if (gameData == null) {
            log.debug("Could not find game on ESPN: {}", prediction.getEventName());
            return null;
        }

        // Check if the game is final
        String status = (String) gameData.get("status");
        if (!"STATUS_FINAL".equals(status)) {
            log.debug("Game not final yet ({}): {}", status, prediction.getEventName());
            return null;
        }

        // Extract final scores (null-safe to avoid NPE from unboxing)
        Object homeObj = gameData.get("homeScore");
        Object awayObj = gameData.get("awayScore");
        if (homeObj == null || awayObj == null) {
            log.warn("Missing score data for game: {}", prediction.getEventName());
            return null;
        }
        int homeScore = ((Number) homeObj).intValue();
        int awayScore = ((Number) awayObj).intValue();

        // Determine the bet result based on bet type and final score
        BetResult result = determineBetResult(prediction, homeScore, awayScore);

        // Create and save the Outcome record
        Outcome outcome = new Outcome();
        outcome.setFinalScoreHome(homeScore);
        outcome.setFinalScoreAway(awayScore);
        outcome.setSource("ESPN");
        outcome.settle(prediction, result);
        outcomeRepository.save(outcome);

        // Save the updated prediction status
        predictionRepository.save(prediction);

        // Update the DailySnapshot tallies including profit
        updateSnapshotTallies(prediction.getSnapshot(), result, outcome.getProfitUnits());

        log.info("Settled: {} → {} (Score: {}-{})",
                prediction.getEventName(), result, homeScore, awayScore);

        return result;
    }

    /**
     * Searches the ESPN scoreboard for a game matching this prediction's teams.
     * Uses the sport's ESPN path to query the correct scoreboard.
     *
     * @return a map with {status, homeScore, awayScore} or null if not found
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> findGameOnEspn(Prediction prediction) {
        try {
            String espnPath = "/" + prediction.getSport().getEspnPath() + "/scoreboard";

            // ESPN only shows today's games by default — pass the game date
            // to fetch the correct day's scoreboard (format: yyyyMMdd)
            String gameDate = prediction.getEventStartTime()
                    .toLocalDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
            log.debug("Fetching ESPN scoreboard: {} for date {}", espnPath, gameDate);

            Map<String, Object> scoreboard = espnClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(espnPath)
                            .queryParam("dates", gameDate)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(java.time.Duration.ofSeconds(10));

            if (scoreboard == null) return null;

            // ESPN response structure: { events: [ { competitions: [ { competitors: [...] } ] } ] }
            List<Map<String, Object>> events = (List<Map<String, Object>>) scoreboard.get("events");
            if (events == null) return null;

            // Search for the matching game by team names
            for (Map<String, Object> event : events) {
                Map<String, Object> match = matchEventToTeams(event,
                        prediction.getHomeTeam(), prediction.getAwayTeam());
                if (match != null) return match;
            }

            return null;
        } catch (Exception e) {
            log.warn("ESPN API error for {}: {}", prediction.getSport(), e.getMessage());
            return null;
        }
    }

    /**
     * Checks if an ESPN event matches the given home and away team names.
     * ESPN uses full team names, so we do a case-insensitive partial match
     * (e.g., "Chiefs" matches "Kansas City Chiefs").
     *
     * @return a map with game data if matched, null otherwise
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> matchEventToTeams(Map<String, Object> event,
                                                   String homeTeam, String awayTeam) {
        List<Map<String, Object>> competitions =
                (List<Map<String, Object>>) event.get("competitions");
        if (competitions == null || competitions.isEmpty()) return null;

        Map<String, Object> competition = competitions.get(0);
        List<Map<String, Object>> competitors =
                (List<Map<String, Object>>) competition.get("competitors");
        if (competitors == null || competitors.size() < 2) return null;

        // ESPN lists home team first (homeAway = "home")
        String espnHomeName = null;
        String espnAwayName = null;
        int espnHomeScore = 0;
        int espnAwayScore = 0;

        for (Map<String, Object> competitor : competitors) {
            Map<String, Object> team = (Map<String, Object>) competitor.get("team");
            String displayName = (String) team.get("displayName");
            String homeAway = (String) competitor.get("homeAway");
            int score = parseScore(competitor.get("score"));

            if ("home".equals(homeAway)) {
                espnHomeName = displayName;
                espnHomeScore = score;
            } else {
                espnAwayName = displayName;
                espnAwayScore = score;
            }
        }

        // Check if team names match (case-insensitive partial match)
        if (teamsMatch(homeTeam, espnHomeName) && teamsMatch(awayTeam, espnAwayName)) {
            // Get game status
            Map<String, Object> statusObj = (Map<String, Object>) competition.get("status");
            Map<String, Object> statusType = statusObj != null
                    ? (Map<String, Object>) statusObj.get("type")
                    : null;
            String statusName = statusType != null
                    ? (String) statusType.get("name")
                    : "UNKNOWN";

            Map<String, Object> result = new HashMap<>();
            result.put("status", statusName);
            result.put("homeScore", espnHomeScore);
            result.put("awayScore", espnAwayScore);
            return result;
        }

        return null;
    }

    /**
     * Determines if a prediction WON, LOST, or PUSHED based on the final score.
     * Handles all bet types: SPREAD, MONEYLINE, TOTAL_OVER, TOTAL_UNDER,
     * and soccer types (HOME_WIN, DRAW, AWAY_WIN).
     */
    private BetResult determineBetResult(Prediction prediction, int homeScore, int awayScore) {
        BetType betType = prediction.getBetType();
        String selection = prediction.getSelection();
        boolean isHomePick = isHomeTeamPick(selection, prediction.getHomeTeam());
        int totalScore = homeScore + awayScore;
        int scoreDiff = homeScore - awayScore; // positive = home winning

        switch (betType) {
            case SPREAD -> {
                // Extract the spread number from the selection (e.g., "Chiefs -3.5" → -3.5)
                double spread = extractLine(selection);

                // If the pick is on the away team, invert the perspective
                double adjustedDiff = isHomePick ? scoreDiff + spread : -scoreDiff + spread;

                if (adjustedDiff > 0) return BetResult.WON;
                if (adjustedDiff < 0) return BetResult.LOST;
                return BetResult.PUSH;
            }
            case MONEYLINE -> {
                if (isHomePick) {
                    if (homeScore > awayScore) return BetResult.WON;
                    return BetResult.LOST;
                } else {
                    if (awayScore > homeScore) return BetResult.WON;
                    return BetResult.LOST;
                }
            }
            case TOTAL_OVER -> {
                double line = extractLine(selection);
                if (totalScore > line) return BetResult.WON;
                if (totalScore < line) return BetResult.LOST;
                return BetResult.PUSH;
            }
            case TOTAL_UNDER -> {
                double line = extractLine(selection);
                if (totalScore < line) return BetResult.WON;
                if (totalScore > line) return BetResult.LOST;
                return BetResult.PUSH;
            }
            case HOME_WIN -> {
                if (homeScore > awayScore) return BetResult.WON;
                return BetResult.LOST;
            }
            case DRAW -> {
                if (homeScore == awayScore) return BetResult.WON;
                return BetResult.LOST;
            }
            case AWAY_WIN -> {
                if (awayScore > homeScore) return BetResult.WON;
                return BetResult.LOST;
            }
            default -> {
                log.warn("Unsupported bet type for auto-settlement: {}", betType);
                return null;
            }
        }
    }

    /**
     * Updates the DailySnapshot win/loss/push tallies and net profit after a prediction is settled.
     */
    private void updateSnapshotTallies(DailySnapshot snapshot, BetResult result, BigDecimal profitUnits) {
        if (snapshot == null) return;

        switch (result) {
            case WON -> snapshot.setWins(snapshot.getWins() + 1);
            case LOST -> snapshot.setLosses(snapshot.getLosses() + 1);
            case PUSH -> snapshot.setPushes(snapshot.getPushes() + 1);
        }

        // Update net profit (was previously missing — profit stayed at stale/zero values)
        BigDecimal currentProfit = snapshot.getNetProfitUnits() != null
                ? snapshot.getNetProfitUnits() : BigDecimal.ZERO;
        snapshot.setNetProfitUnits(currentProfit.add(
                profitUnits != null ? profitUnits : BigDecimal.ZERO));

        snapshot.recalculateStats();
        snapshotRepository.save(snapshot);
    }

    // === Parsing Helpers ===

    /**
     * Checks if a selection string refers to the home team.
     * Does a case-insensitive partial match against the home team name.
     */
    private boolean isHomeTeamPick(String selection, String homeTeam) {
        if (selection == null || homeTeam == null) return true;
        String selLower = selection.toLowerCase();
        for (String word : homeTeam.toLowerCase().split("\\s+")) {
            if (word.length() > 2 && selLower.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts a numeric line/spread from a selection string.
     * Examples: "Chiefs -3.5" → -3.5, "Over 48.5" → 48.5, "Under 215" → 215
     */
    private double extractLine(String selection) {
        if (selection == null) return 0;
        Matcher matcher = LINE_PATTERN.matcher(selection);
        double lastNumber = 0;
        while (matcher.find()) {
            try {
                lastNumber = Double.parseDouble(matcher.group());
            } catch (NumberFormatException ignored) {
            }
        }
        return lastNumber;
    }

    /**
     * Checks if two team names match using case-insensitive partial matching.
     * Handles differences like "KC Chiefs" vs "Kansas City Chiefs" by checking
     * if any significant word (3+ chars) from one name appears in the other.
     */
    private boolean teamsMatch(String ourTeam, String espnTeam) {
        if (ourTeam == null || espnTeam == null) return false;
        String ours = ourTeam.toLowerCase();
        String theirs = espnTeam.toLowerCase();

        // Exact match
        if (ours.equals(theirs)) return true;

        // Check if any significant word matches
        for (String word : ours.split("\\s+")) {
            if (word.length() > 2 && theirs.contains(word)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely parses a score value from ESPN's response.
     * ESPN sometimes returns scores as strings.
     */
    private int parseScore(Object scoreObj) {
        if (scoreObj == null) return 0;
        try {
            return Integer.parseInt(scoreObj.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
