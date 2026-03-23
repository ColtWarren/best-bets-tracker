package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.repository.DailySnapshotRepository;
import com.coltwarren.best_bets_tracker.repository.OutcomeRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages DailySnapshot lifecycle — retrieval, stats recalculation,
 * and aggregate queries across all snapshots.
 *
 * DailySnapshots are the "day-level" view of the tracker. Each day
 * has one snapshot containing all predictions captured that day,
 * with running tallies of wins/losses/pushes.
 */
@Service
public class DailySnapshotService {

    private static final Logger log = LoggerFactory.getLogger(DailySnapshotService.class);

    private final DailySnapshotRepository snapshotRepository;
    private final PredictionRepository predictionRepository;
    private final OutcomeRepository outcomeRepository;

    public DailySnapshotService(DailySnapshotRepository snapshotRepository,
                                PredictionRepository predictionRepository,
                                OutcomeRepository outcomeRepository) {
        this.snapshotRepository = snapshotRepository;
        this.predictionRepository = predictionRepository;
        this.outcomeRepository = outcomeRepository;
    }

    /**
     * Returns the snapshot for a specific date, or empty if none exists.
     *
     * @param date the date to look up
     * @return Optional containing the snapshot, if found
     */
    public Optional<DailySnapshot> getByDate(LocalDate date) {
        return snapshotRepository.findBySnapshotDate(date);
    }

    /**
     * Returns all snapshots ordered by date descending (most recent first).
     * This powers the "daily history" view on the dashboard.
     *
     * @return all snapshots, newest first
     */
    public List<DailySnapshot> getAllOrderedByDate() {
        return snapshotRepository.findAllOrderByDateDesc();
    }

    /**
     * Returns snapshots within a date range.
     * Useful for weekly/monthly rollup views.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     * @return snapshots in the range
     */
    public List<DailySnapshot> getByDateRange(LocalDate start, LocalDate end) {
        return snapshotRepository.findBySnapshotDateBetween(start, end);
    }

    /**
     * Returns snapshots that have unresolved predictions (games not yet settled).
     * Used by the scheduled resolution job to know which days still need work.
     *
     * @return list of snapshots with pending picks before today
     */
    public List<DailySnapshot> getUnresolved() {
        return snapshotRepository.findUnresolvedBefore(LocalDate.now());
    }

    /**
     * Recalculates the win/loss/push tallies and net profit for a snapshot
     * by re-examining all of its predictions' current statuses.
     *
     * Call this if you suspect the tallies are out of sync, or after
     * manual bet settlement.
     *
     * @param snapshotId the ID of the snapshot to recalculate
     * @return the updated snapshot
     */
    @Transactional
    public DailySnapshot recalculateStats(Long snapshotId) {
        DailySnapshot snapshot = snapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new RuntimeException("Snapshot not found: " + snapshotId));

        List<Prediction> predictions = predictionRepository.findBySnapshotId(snapshotId);

        int wins = 0, losses = 0, pushes = 0;
        BigDecimal netProfit = BigDecimal.ZERO;

        for (Prediction p : predictions) {
            switch (p.getStatus()) {
                case WON -> {
                    wins++;
                    netProfit = netProfit.add(p.calculateProfitUnits());
                }
                case LOST -> {
                    losses++;
                    netProfit = netProfit.add(BigDecimal.ONE.negate());
                }
                case PUSH -> pushes++;
                default -> { /* still pending */ }
            }
        }

        snapshot.setWins(wins);
        snapshot.setLosses(losses);
        snapshot.setPushes(pushes);
        snapshot.setNetProfitUnits(netProfit);
        snapshot.recalculateStats();

        log.info("Recalculated snapshot {}: {}-{}-{}, net: {} units",
                snapshot.getSnapshotDate(), wins, losses, pushes, netProfit);

        return snapshotRepository.save(snapshot);
    }

    /**
     * Returns aggregate stats across all snapshots.
     * Used for the "all-time" summary on the dashboard header.
     *
     * @return map with totalDays, totalPicks, totalWins, totalLosses, totalProfit, overallWinRate
     */
    public Map<String, Object> getAggregateStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalPicks = snapshotRepository.sumAllPicks();
        long totalWins = snapshotRepository.sumAllWins();
        long totalLosses = snapshotRepository.sumAllLosses();
        BigDecimal totalProfit = snapshotRepository.sumAllProfitUnits();
        long totalDays = snapshotRepository.count();

        double winRate = 0;
        long settled = totalWins + totalLosses;
        if (settled > 0) {
            winRate = (double) totalWins / settled * 100;
        }

        stats.put("totalDays", totalDays);
        stats.put("totalPicks", totalPicks);
        stats.put("totalWins", totalWins);
        stats.put("totalLosses", totalLosses);
        stats.put("settled", settled);
        stats.put("overallWinRate", Math.round(winRate * 100.0) / 100.0);
        stats.put("totalProfit", totalProfit);

        return stats;
    }

    /**
     * Calculates the current winning/losing streak across all days.
     * A "streak" is the number of consecutive profitable or unprofitable days.
     *
     * @return map with {type: "WIN_STREAK" or "LOSS_STREAK", count: N}
     */
    public Map<String, Object> getCurrentStreak() {
        List<DailySnapshot> resolved = snapshotRepository.findResolvedDaysDesc();

        Map<String, Object> streak = new HashMap<>();
        if (resolved.isEmpty()) {
            streak.put("type", "NONE");
            streak.put("count", 0);
            return streak;
        }

        // Determine if the most recent day was profitable or not
        boolean firstDayProfit = resolved.get(0).getNetProfitUnits() != null
                && resolved.get(0).getNetProfitUnits().compareTo(BigDecimal.ZERO) > 0;

        int count = 0;
        for (DailySnapshot day : resolved) {
            boolean dayProfit = day.getNetProfitUnits() != null
                    && day.getNetProfitUnits().compareTo(BigDecimal.ZERO) > 0;

            if (dayProfit == firstDayProfit) {
                count++;
            } else {
                break;
            }
        }

        streak.put("type", firstDayProfit ? "WIN_STREAK" : "LOSS_STREAK");
        streak.put("count", count);
        return streak;
    }
}
