package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.dto.MlbAssessmentResponseDTO;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Captures MLB assessment predictions from the main sports-betting-analytics app
 * by calling its /api/mlb/assessments endpoint.
 *
 * Each assessment is mapped to a Prediction entity with assessmentType = "MLB_ASSESSMENT".
 * Runs once daily (via scheduled job or manual trigger) to snapshot that day's picks.
 */
@Service
public class MlbAssessmentCaptureService {

    private static final Logger log = LoggerFactory.getLogger(MlbAssessmentCaptureService.class);

    private final WebClient webClient;
    private final PredictionRepository predictionRepository;
    private final DailySnapshotRepository snapshotRepository;

    public MlbAssessmentCaptureService(
            @Value("${main-app.base-url}") String mainAppBaseUrl,
            @Value("${internal.api.key:}") String internalApiKey,
            PredictionRepository predictionRepository,
            DailySnapshotRepository snapshotRepository) {
        this.webClient = WebClient.builder()
                .baseUrl(mainAppBaseUrl)
                .defaultHeader("X-Internal-API-Key", internalApiKey)
                .codecs(config -> config.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
        this.predictionRepository = predictionRepository;
        this.snapshotRepository = snapshotRepository;
    }

    /**
     * Captures today's MLB assessments from the main app.
     *
     * @return count of saved vs skipped predictions
     */
    @Transactional
    public CaptureResult captureToday() {
        return captureForDate(LocalDate.now());
    }

    /**
     * Captures MLB assessments for a specific date.
     *
     * @param date the date to associate with this snapshot
     * @return count of saved vs skipped predictions
     */
    @Transactional
    public CaptureResult captureForDate(LocalDate date) {
        log.info("Capturing MLB assessments for date: {}", date);

        List<MlbAssessmentResponseDTO> assessments = fetchAssessmentsFromMainApp();
        if (assessments == null || assessments.isEmpty()) {
            log.warn("No MLB assessments returned from main app for date: {}", date);
            return new CaptureResult(0, 0);
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

        List<Prediction> newPredictions = new ArrayList<>();
        int savedCount = 0;
        int skippedCount = 0;

        for (MlbAssessmentResponseDTO assessment : assessments) {
            try {
                String homeTeam = assessment.getHomeTeam();
                String awayTeam = assessment.getAwayTeam();
                String selection = assessment.getVerdict();
                LocalDateTime eventStartTime = LocalDateTime.now().plusHours(6);

                // Dedup check — same matchup + selection + event time
                if (predictionRepository.existsByHomeTeamIgnoreCaseAndAwayTeamIgnoreCaseAndSelectionIgnoreCaseAndEventStartTime(
                        homeTeam, awayTeam, selection, eventStartTime)) {
                    log.debug("Skipping duplicate: {} @ {} - {}", awayTeam, homeTeam, selection);
                    skippedCount++;
                    continue;
                }

                Prediction prediction = mapAssessmentToPrediction(assessment, snapshot, eventStartTime);
                newPredictions.add(prediction);
                savedCount++;
            } catch (Exception e) {
                log.warn("Failed to map assessment: {} @ {}. Error: {}",
                        assessment.getAwayTeam(), assessment.getHomeTeam(), e.getMessage());
            }
        }

        // Save snapshot first (so it has an ID for the FK), then save predictions
        snapshot.setTotalPicks(snapshot.getTotalPicks() + savedCount);
        snapshot = snapshotRepository.save(snapshot);

        if (!newPredictions.isEmpty()) {
            predictionRepository.saveAll(newPredictions);
        }

        log.info("Captured {} new MLB assessments for {} ({} duplicates skipped)",
                savedCount, date, skippedCount);

        return new CaptureResult(savedCount, skippedCount);
    }

    /**
     * Calls GET /api/mlb/assessments on the main app.
     *
     * @return list of MLB assessment DTOs, or null on failure
     */
    private List<MlbAssessmentResponseDTO> fetchAssessmentsFromMainApp() {
        try {
            log.debug("Fetching MLB assessments from main app...");
            return webClient.get()
                    .uri("/api/mlb/assessments")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<MlbAssessmentResponseDTO>>() {})
                    .block(java.time.Duration.ofSeconds(60));
        } catch (Exception e) {
            log.error("Error fetching MLB assessments from main app: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Maps an MlbAssessmentResponseDTO to a Prediction entity.
     */
    private Prediction mapAssessmentToPrediction(MlbAssessmentResponseDTO assessment,
                                                  DailySnapshot snapshot,
                                                  LocalDateTime eventStartTime) {
        Prediction p = new Prediction();
        p.setSnapshot(snapshot);

        // Game info
        p.setSport(Sport.MLB);
        p.setHomeTeam(assessment.getHomeTeam());
        p.setAwayTeam(assessment.getAwayTeam());
        p.setEventName(assessment.getAwayTeam() + " @ " + assessment.getHomeTeam()
                + " (" + assessment.getVenue() + ")");
        p.setEventStartTime(eventStartTime);

        // AI recommendation
        p.setSelection(assessment.getVerdict());
        p.setConfidence(assessment.getVerdictConfidence() != null
                ? BigDecimal.valueOf(assessment.getVerdictConfidence())
                : BigDecimal.ZERO);
        p.setAiReasoning(assessment.getReasoning());

        // Bet type from bettingType field
        p.setBetType(inferBetType(assessment.getBettingType()));

        // Odds — use home moneyline as default
        p.setOdds(assessment.getMoneylineHome() != null
                ? BigDecimal.valueOf(assessment.getMoneylineHome())
                : BigDecimal.ZERO);
        p.setSportsbook("Best Available");

        // Assessment type
        p.setAssessmentType("MLB_ASSESSMENT");

        // Default status
        p.setStatus(BetResult.PENDING);
        p.setPredictionTime(LocalDateTime.now());

        return p;
    }

    /**
     * Infers BetType from the assessment's bettingType string.
     * "TOTAL" alone (without over/under direction) defaults to MONEYLINE
     * since we don't know the direction.
     */
    private BetType inferBetType(String bettingType) {
        if (bettingType == null) return BetType.MONEYLINE;

        String lower = bettingType.toLowerCase();
        if (lower.equals("total")) return BetType.MONEYLINE;
        if (lower.contains("over")) return BetType.TOTAL_OVER;
        if (lower.contains("under")) return BetType.TOTAL_UNDER;
        if (lower.contains("spread")) return BetType.SPREAD;
        return BetType.MONEYLINE;
    }

    /**
     * Result of a capture operation: how many were saved vs skipped.
     */
    public record CaptureResult(int saved, int skipped) {}
}
