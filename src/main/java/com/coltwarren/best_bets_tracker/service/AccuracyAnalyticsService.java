package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.model.AccuracyReport;
import com.coltwarren.best_bets_tracker.model.enums.BetType;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import com.coltwarren.best_bets_tracker.repository.AccuracyReportRepository;
import com.coltwarren.best_bets_tracker.repository.OutcomeRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Computes accuracy and profitability analytics across all predictions.
 * This is the core service that answers the big question:
 * "Are these AI picks good enough to bet real money?"
 *
 * Provides:
 * - Overall accuracy (win rate, ROI, net units)
 * - Breakdown by sport (which sports does the AI do best on?)
 * - Breakdown by bet type (spreads vs moneylines vs totals)
 * - Breakdown by confidence tier (do high-confidence picks hit more?)
 * - CLV analysis (does the AI consistently beat closing lines?)
 * - Trend analysis (is accuracy improving or declining?)
 * - Pre-computed reports for fast dashboard rendering
 */
@Service
public class AccuracyAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AccuracyAnalyticsService.class);

    private final PredictionRepository predictionRepository;
    private final OutcomeRepository outcomeRepository;
    private final AccuracyReportRepository reportRepository;

    public AccuracyAnalyticsService(PredictionRepository predictionRepository,
                                    OutcomeRepository outcomeRepository,
                                    AccuracyReportRepository reportRepository) {
        this.predictionRepository = predictionRepository;
        this.outcomeRepository = outcomeRepository;
        this.reportRepository = reportRepository;
    }

    // =========================================================================
    // OVERALL STATS — the headline numbers for the dashboard
    // =========================================================================

    /**
     * Returns the top-level accuracy stats across all predictions.
     * This is the first thing a user sees on the dashboard.
     *
     * @return map with winRate, totalPicks, wins, losses, pushes, roi, netUnits, clvRate
     */
    public Map<String, Object> getOverallStats() {
        Map<String, Object> stats = new HashMap<>();

        long wins = predictionRepository.countWins();
        long losses = predictionRepository.countLosses();
        long settled = predictionRepository.countSettled();
        long totalPicks = predictionRepository.count();
        long pending = predictionRepository.countByStatus(
                com.coltwarren.best_bets_tracker.model.enums.BetResult.PENDING);

        // Win rate: wins / (wins + losses) — pushes excluded
        Double winRate = predictionRepository.calculateOverallWinRate();
        BigDecimal netUnits = outcomeRepository.calculateTotalProfitUnits();

        // ROI: net profit / total units wagered * 100
        // Each pick is 1 unit, so total wagered = number of settled picks
        BigDecimal roi = BigDecimal.ZERO;
        if (settled > 0) {
            roi = netUnits
                    .divide(BigDecimal.valueOf(settled), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // CLV: percentage of settled picks that beat the closing line
        long clvCount = predictionRepository.countBeatClosingLine();
        long clvTotal = predictionRepository.countWithClosingOdds();
        BigDecimal clvRate = BigDecimal.ZERO;
        if (clvTotal > 0) {
            clvRate = BigDecimal.valueOf(clvCount)
                    .divide(BigDecimal.valueOf(clvTotal), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        stats.put("totalPicks", totalPicks);
        stats.put("settled", settled);
        stats.put("pending", pending);
        stats.put("wins", wins);
        stats.put("losses", losses);
        stats.put("pushes", settled - wins - losses);
        stats.put("winRate", winRate != null ? round(winRate * 100) : 0.0);
        stats.put("netUnits", netUnits);
        stats.put("roi", roi);
        stats.put("clvRate", clvRate);
        stats.put("avgOdds", predictionRepository.calculateAvgOdds());
        stats.put("avgConfidence", predictionRepository.calculateAvgConfidence());

        return stats;
    }

    // =========================================================================
    // BY SPORT — which sports does the AI predict best?
    // =========================================================================

    /**
     * Returns accuracy breakdown for each sport that has settled predictions.
     * Sorted by win rate descending so the best-performing sports are first.
     *
     * @return list of maps, each with {sport, wins, losses, winRate, netUnits, avgConfidence}
     */
    public List<Map<String, Object>> getStatsBySport() {
        List<Map<String, Object>> sportStats = new ArrayList<>();

        for (Sport sport : Sport.values()) {
            long wins = predictionRepository.countWinsBySport(sport);
            long losses = predictionRepository.countLossesBySport(sport);
            long settled = wins + losses;

            // Skip sports with no data
            if (settled == 0) continue;

            Double winRate = predictionRepository.calculateWinRateBySport(sport);
            BigDecimal netUnits = outcomeRepository.calculateProfitUnitsBySport(sport);
            Double avgConf = predictionRepository.calculateAvgConfidenceBySport(sport);

            BigDecimal roi = netUnits
                    .divide(BigDecimal.valueOf(settled), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> entry = new HashMap<>();
            entry.put("sport", sport.name());
            entry.put("wins", wins);
            entry.put("losses", losses);
            entry.put("settled", settled);
            entry.put("winRate", winRate != null ? round(winRate * 100) : 0.0);
            entry.put("netUnits", netUnits);
            entry.put("roi", roi);
            entry.put("avgConfidence", avgConf != null ? round(avgConf) : 0.0);

            sportStats.add(entry);
        }

        // Sort by win rate descending
        sportStats.sort((a, b) -> Double.compare(
                (double) b.get("winRate"), (double) a.get("winRate")));

        return sportStats;
    }

    // =========================================================================
    // BY BET TYPE — spreads vs moneylines vs totals
    // =========================================================================

    /**
     * Returns accuracy breakdown for each bet type.
     * Helps identify if the AI is better at picking spreads, totals, or moneylines.
     *
     * @return list of maps, each with {betType, winRate, settled}
     */
    public List<Map<String, Object>> getStatsByBetType() {
        List<Map<String, Object>> typeStats = new ArrayList<>();

        for (BetType betType : BetType.values()) {
            Double winRate = predictionRepository.calculateWinRateByBetType(betType);
            if (winRate == null) continue;

            Map<String, Object> entry = new HashMap<>();
            entry.put("betType", betType.name());
            entry.put("winRate", round(winRate * 100));
            typeStats.add(entry);
        }

        typeStats.sort((a, b) -> Double.compare(
                (double) b.get("winRate"), (double) a.get("winRate")));

        return typeStats;
    }

    // =========================================================================
    // BY CONFIDENCE TIER — do higher-confidence picks hit more often?
    // =========================================================================

    /**
     * Returns accuracy breakdown by confidence tiers.
     * This is critical for determining a betting strategy — e.g., only bet
     * on picks with confidence 8+.
     *
     * Tiers: 6-7, 7-8, 8-9, 9-10
     *
     * @return list of maps, each with {tier, minConf, maxConf, winRate, settled, wins, losses}
     */
    public List<Map<String, Object>> getStatsByConfidenceTier() {
        List<Map<String, Object>> tiers = new ArrayList<>();

        // Define tiers: [min, max) — min inclusive, max exclusive
        double[][] tierBounds = {
                {6.0, 7.0},
                {7.0, 8.0},
                {8.0, 9.0},
                {9.0, 10.1}  // 10.1 to include 10.0
        };
        String[] tierLabels = {"6-7 (Low)", "7-8 (Medium)", "8-9 (High)", "9-10 (Elite)"};

        for (int i = 0; i < tierBounds.length; i++) {
            BigDecimal minConf = BigDecimal.valueOf(tierBounds[i][0]);
            BigDecimal maxConf = BigDecimal.valueOf(tierBounds[i][1]);

            Double winRate = predictionRepository.calculateWinRateByConfidenceRange(minConf, maxConf);
            long settled = predictionRepository.countSettledByConfidenceRange(minConf, maxConf);

            if (settled == 0) continue;

            Map<String, Object> entry = new HashMap<>();
            entry.put("tier", tierLabels[i]);
            entry.put("minConf", tierBounds[i][0]);
            entry.put("maxConf", tierBounds[i][1]);
            entry.put("winRate", winRate != null ? round(winRate * 100) : 0.0);
            entry.put("settled", settled);

            tiers.add(entry);
        }

        return tiers;
    }

    // =========================================================================
    // TREND ANALYSIS — is accuracy improving or declining?
    // =========================================================================

    /**
     * Returns win rate calculated over a rolling window.
     * Compare recent performance vs all-time to spot trends.
     *
     * @param days number of days to look back
     * @return map with {period, winRate, wins, losses}
     */
    public Map<String, Object> getTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Double winRate = predictionRepository.calculateWinRateSince(since);
        long wins = predictionRepository.countWinsSince(since);
        long losses = predictionRepository.countLossesSince(since);

        Map<String, Object> trend = new HashMap<>();
        trend.put("period", "Last " + days + " days");
        trend.put("winRate", winRate != null ? round(winRate * 100) : 0.0);
        trend.put("wins", wins);
        trend.put("losses", losses);
        trend.put("settled", wins + losses);
        trend.put("netUnits", outcomeRepository.calculateProfitUnitsSince(since));

        return trend;
    }

    /**
     * Returns multiple trend windows for comparison.
     * Shows whether the AI is getting better or worse over time.
     *
     * @return list of trend maps for 7, 14, 30, and 90 day windows
     */
    public List<Map<String, Object>> getTrendComparison() {
        List<Map<String, Object>> trends = new ArrayList<>();
        for (int days : new int[]{7, 14, 30, 90}) {
            trends.add(getTrend(days));
        }
        return trends;
    }

    // =========================================================================
    // REPORT GENERATION — pre-compute stats for fast dashboard loading
    // =========================================================================

    /**
     * Generates and saves an all-time accuracy report.
     * Call this after settling new outcomes to keep the dashboard up to date.
     */
    @Transactional
    public AccuracyReport generateAllTimeReport() {
        Map<String, Object> stats = getOverallStats();
        return saveReport("ALL_TIME", LocalDate.of(2020, 1, 1), LocalDate.now(), null, stats);
    }

    /**
     * Generates and saves accuracy reports for each sport.
     * Creates one AccuracyReport per sport with settled data.
     */
    @Transactional
    public List<AccuracyReport> generateSportReports() {
        List<AccuracyReport> reports = new ArrayList<>();
        for (Map<String, Object> sportData : getStatsBySport()) {
            Sport sport = Sport.valueOf((String) sportData.get("sport"));
            AccuracyReport report = saveReport("ALL_TIME",
                    LocalDate.of(2020, 1, 1), LocalDate.now(), sport, sportData);
            reports.add(report);
        }
        return reports;
    }

    /**
     * Generates a daily report for the given date.
     * Stores a snapshot of accuracy as of that day.
     */
    @Transactional
    public AccuracyReport generateDailyReport(LocalDate date) {
        Map<String, Object> stats = getOverallStats();
        return saveReport("DAILY", date, date, null, stats);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * Saves (or updates) an AccuracyReport with the given stats.
     * If a report with the same type/period/sport already exists, it gets updated
     * rather than creating a duplicate.
     */
    private AccuracyReport saveReport(String reportType, LocalDate periodStart,
                                       LocalDate periodEnd, Sport sport,
                                       Map<String, Object> stats) {
        // Look for existing report to update
        AccuracyReport report = reportRepository
                .findByReportTypeAndPeriodStartAndPeriodEndAndSport(
                        reportType, periodStart, periodEnd, sport)
                .orElse(new AccuracyReport());

        report.setReportType(reportType);
        report.setPeriodStart(periodStart);
        report.setPeriodEnd(periodEnd);
        report.setSport(sport);

        report.setTotalPicks(getIntStat(stats, "settled"));
        report.setWins(getIntStat(stats, "wins"));
        report.setLosses(getIntStat(stats, "losses"));
        report.setPushes(getIntStat(stats, "pushes"));
        report.setWinRate(getBigDecimalStat(stats, "winRate"));
        report.setRoiPercent(getBigDecimalStat(stats, "roi"));
        report.setNetUnits(getBigDecimalStat(stats, "netUnits"));
        report.setAvgOdds(getBigDecimalStat(stats, "avgOdds"));
        report.setAvgConfidence(getBigDecimalStat(stats, "avgConfidence"));
        report.setClvRate(getBigDecimalStat(stats, "clvRate"));
        report.setGeneratedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }

    /**
     * Rounds a double to 2 decimal places.
     */
    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Safely extracts an int value from the stats map.
     */
    private int getIntStat(Map<String, Object> stats, String key) {
        Object val = stats.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        return 0;
    }

    /**
     * Safely extracts a BigDecimal value from the stats map.
     */
    private BigDecimal getBigDecimalStat(Map<String, Object> stats, String key) {
        Object val = stats.get(key);
        if (val instanceof BigDecimal) return (BigDecimal) val;
        if (val instanceof Number) return BigDecimal.valueOf(((Number) val).doubleValue());
        return null;
    }
}
