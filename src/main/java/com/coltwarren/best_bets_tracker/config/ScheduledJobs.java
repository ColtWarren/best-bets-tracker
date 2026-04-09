package com.coltwarren.best_bets_tracker.config;

import com.coltwarren.best_bets_tracker.service.AccuracyAnalyticsService;
import com.coltwarren.best_bets_tracker.service.MlbAssessmentCaptureService;
import com.coltwarren.best_bets_tracker.service.OutcomeResolutionService;
import com.coltwarren.best_bets_tracker.service.PredictionCaptureService;
import com.coltwarren.best_bets_tracker.service.SimulationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Scheduled background jobs that keep the tracker running automatically.
 *
 * All cron expressions use Central Time (America/Chicago) since
 * the user is based in Missouri and most US sports events are
 * scheduled around CT/ET.
 *
 * Jobs:
 * 1. Daily capture — pull AI best bets from the main app each morning
 * 2. Outcome resolution — check ESPN for final scores every 30 minutes
 * 3. Weekly report — generate accuracy reports every Monday morning
 */
@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    /**
     * Default flat stake for auto-created simulated bets ($50 per bet).
     */
    private static final BigDecimal DEFAULT_FLAT_STAKE = BigDecimal.valueOf(50);

    private final PredictionCaptureService captureService;
    private final OutcomeResolutionService resolutionService;
    private final AccuracyAnalyticsService analyticsService;
    private final SimulationService simulationService;
    private final MlbAssessmentCaptureService mlbAssessmentCaptureService;

    public ScheduledJobs(PredictionCaptureService captureService,
                         OutcomeResolutionService resolutionService,
                         AccuracyAnalyticsService analyticsService,
                         SimulationService simulationService,
                         MlbAssessmentCaptureService mlbAssessmentCaptureService) {
        this.captureService = captureService;
        this.resolutionService = resolutionService;
        this.analyticsService = analyticsService;
        this.simulationService = simulationService;
        this.mlbAssessmentCaptureService = mlbAssessmentCaptureService;
    }

    /**
     * DAILY CAPTURE — Runs at 9:00 AM Central Time every day.
     *
     * Pulls the current best bets from the main sports-betting-analytics
     * app and stores them as predictions. Also creates simulated bets
     * for each new prediction using flat staking.
     *
     * 9 AM CT is chosen because:
     * - Most sportsbooks have posted daily lines by then
     * - The main app's odds cache has been refreshed overnight
     * - Early enough to capture full-day slates (NBA/NHL typically start ~6 PM CT)
     *
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "America/Chicago")
    public void captureDailyBestBets() {
        log.info("=== SCHEDULED: Daily best bets capture ===");
        try {
            var snapshot = captureService.captureToday();
            log.info("Captured {} predictions for {}", snapshot.getTotalPicks(), snapshot.getSnapshotDate());

            // Auto-create simulated bets for the new predictions
            int simBets = simulationService.createBetsForAllPending(DEFAULT_FLAT_STAKE, null);
            log.info("Created {} simulated bets", simBets);
        } catch (Exception e) {
            log.error("Daily capture failed: {}", e.getMessage(), e);
        }
    }

    /**
     * MLB ASSESSMENT CAPTURE — Runs at 11:00 AM Central Time every day.
     *
     * Pulls MLB matchup assessments from the main sports-betting-analytics
     * app and stores them as predictions with assessmentType = "MLB_ASSESSMENT".
     *
     * 11 AM CT is chosen because:
     * - Starting pitchers are typically confirmed by then
     * - MLB games usually start ~1-7 PM CT, giving time for pre-game analysis
     * - Runs after the 9 AM general capture to avoid overlap
     *
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 11 * * *", zone = "America/Chicago")
    public void captureMlbAssessments() {
        log.info("=== SCHEDULED: MLB assessment capture ===");
        try {
            var result = mlbAssessmentCaptureService.captureToday();
            log.info("MLB assessments: {} saved, {} skipped", result.saved(), result.skipped());
        } catch (Exception e) {
            log.error("MLB assessment capture failed: {}", e.getMessage(), e);
        }
    }

    /**
     * OUTCOME RESOLUTION — Runs every 30 minutes.
     *
     * Checks ESPN's scoreboard API for final scores on games that
     * started 4+ hours ago. Settles predictions as WON/LOST/PUSH
     * and updates daily snapshot tallies.
     *
     * Also settles any pending simulated bets whose predictions
     * have been resolved.
     *
     * 30-minute interval balances:
     * - Timely resolution (most games are settled within an hour of ending)
     * - ESPN API courtesy (no auth required, but don't hammer it)
     */
    @Scheduled(fixedRate = 1800000)  // 30 minutes in milliseconds
    public void resolveOutcomes() {
        log.info("=== SCHEDULED: Outcome resolution ===");
        try {
            Map<String, Object> result = resolutionService.resolveAll();
            int settled = (int) result.getOrDefault("settled", 0);
            int stillPending = (int) result.getOrDefault("stillPending", 0);

            if (settled > 0) {
                log.info("Settled {} predictions, {} still pending", settled, stillPending);

                // Settle corresponding simulated bets
                int simSettled = simulationService.settleResolvedBets();
                log.info("Settled {} simulated bets", simSettled);
            } else {
                log.debug("No predictions settled this cycle ({} still pending)", stillPending);
            }
        } catch (Exception e) {
            log.error("Outcome resolution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * WEEKLY REPORT — Runs at 7:00 AM Central Time every Monday.
     *
     * Generates pre-computed accuracy reports:
     * - All-time overall report
     * - Per-sport reports
     * - Daily report for the previous day
     *
     * These reports are cached in the accuracy_reports table so the
     * dashboard can load instantly without re-computing from raw data.
     *
     * Monday 7 AM is chosen because:
     * - Full weekend of games (NFL Sunday, etc.) has been settled
     * - Reports are ready before the start of the work week
     */
    @Scheduled(cron = "0 0 7 * * MON", zone = "America/Chicago")
    public void generateWeeklyReports() {
        log.info("=== SCHEDULED: Weekly report generation ===");
        try {
            // Generate all-time report
            analyticsService.generateAllTimeReport();
            log.info("Generated all-time accuracy report");

            // Generate per-sport reports
            var sportReports = analyticsService.generateSportReports();
            log.info("Generated {} sport-specific reports", sportReports.size());

            // Generate daily report for yesterday
            analyticsService.generateDailyReport(LocalDate.now().minusDays(1));
            log.info("Generated daily report for {}", LocalDate.now().minusDays(1));

        } catch (Exception e) {
            log.error("Weekly report generation failed: {}", e.getMessage(), e);
        }
    }
}
