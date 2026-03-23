package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.service.AccuracyAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for accuracy and profitability analytics.
 *
 * These endpoints answer the central question:
 * "Are the AI's best bet recommendations good enough to bet real money?"
 *
 * All stats are computed in real-time from the database.
 * For cached/pre-computed stats, use the report generation endpoints.
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AccuracyAnalyticsService analyticsService;

    public AnalyticsController(AccuracyAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * GET /api/analytics/accuracy
     *
     * Returns overall accuracy stats: win rate, ROI, net units,
     * CLV rate, average odds, and average confidence.
     * This is the primary dashboard endpoint.
     */
    @GetMapping("/accuracy")
    public ResponseEntity<Map<String, Object>> getOverallAccuracy() {
        return ResponseEntity.ok(analyticsService.getOverallStats());
    }

    /**
     * GET /api/analytics/accuracy/by-sport
     *
     * Returns accuracy breakdown by sport. Shows which sports
     * the AI predicts best (sorted by win rate descending).
     *
     * Response: [{ sport, wins, losses, winRate, netUnits, roi, avgConfidence }, ...]
     */
    @GetMapping("/accuracy/by-sport")
    public ResponseEntity<List<Map<String, Object>>> getAccuracyBySport() {
        return ResponseEntity.ok(analyticsService.getStatsBySport());
    }

    /**
     * GET /api/analytics/accuracy/by-type
     *
     * Returns accuracy breakdown by bet type (SPREAD, MONEYLINE,
     * TOTAL_OVER, TOTAL_UNDER, etc.). Shows which bet types
     * the AI is most accurate on.
     *
     * Response: [{ betType, winRate }, ...]
     */
    @GetMapping("/accuracy/by-type")
    public ResponseEntity<List<Map<String, Object>>> getAccuracyByBetType() {
        return ResponseEntity.ok(analyticsService.getStatsByBetType());
    }

    /**
     * GET /api/analytics/accuracy/by-confidence
     *
     * Returns accuracy breakdown by confidence tier.
     * This is critical for determining a betting threshold — e.g.,
     * "only bet on picks with confidence 8+".
     *
     * Tiers: 6-7 (Low), 7-8 (Medium), 8-9 (High), 9-10 (Elite)
     *
     * Response: [{ tier, minConf, maxConf, winRate, settled }, ...]
     */
    @GetMapping("/accuracy/by-confidence")
    public ResponseEntity<List<Map<String, Object>>> getAccuracyByConfidence() {
        return ResponseEntity.ok(analyticsService.getStatsByConfidenceTier());
    }

    /**
     * GET /api/analytics/trend?days=30
     *
     * Returns accuracy stats for a rolling time window.
     * Compare to overall stats to spot improvement or decline.
     *
     * @param days number of days to look back (default 30)
     */
    @GetMapping("/trend")
    public ResponseEntity<Map<String, Object>> getTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getTrend(days));
    }

    /**
     * GET /api/analytics/trend/comparison
     *
     * Returns win rate and profit across multiple time windows:
     * 7 days, 14 days, 30 days, 90 days.
     * Shows whether the AI is getting better or worse over time.
     */
    @GetMapping("/trend/comparison")
    public ResponseEntity<List<Map<String, Object>>> getTrendComparison() {
        return ResponseEntity.ok(analyticsService.getTrendComparison());
    }

    /**
     * GET /api/analytics/dashboard
     *
     * Returns a combined payload with all dashboard data in one call.
     * Reduces the number of API calls the React frontend needs to make.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> dashboard = new HashMap<>();
        dashboard.put("overall", analyticsService.getOverallStats());
        dashboard.put("bySport", analyticsService.getStatsBySport());
        dashboard.put("byBetType", analyticsService.getStatsByBetType());
        dashboard.put("byConfidence", analyticsService.getStatsByConfidenceTier());
        dashboard.put("trends", analyticsService.getTrendComparison());
        return ResponseEntity.ok(dashboard);
    }

    /**
     * POST /api/analytics/reports/generate
     *
     * Generates and saves pre-computed accuracy reports (all-time + per-sport).
     * Call this after settling outcomes to refresh the cached reports.
     */
    @PostMapping("/reports/generate")
    public ResponseEntity<Map<String, Object>> generateReports() {
        try {
            analyticsService.generateAllTimeReport();
            var sportReports = analyticsService.generateSportReports();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "reportsGenerated", 1 + sportReports.size(),
                    "message", "Generated all-time report + " + sportReports.size() + " sport reports"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}
