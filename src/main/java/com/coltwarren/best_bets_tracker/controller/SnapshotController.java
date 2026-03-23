package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import com.coltwarren.best_bets_tracker.service.DailySnapshotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for daily snapshots — the day-level view of predictions.
 *
 * Each snapshot represents one day's batch of AI recommendations
 * with running tallies of wins, losses, pushes, and net profit.
 */
@RestController
@RequestMapping("/api/snapshots")
public class SnapshotController {

    private final DailySnapshotService snapshotService;

    public SnapshotController(DailySnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    /**
     * GET /api/snapshots
     *
     * Returns all daily snapshots ordered by date descending.
     * Powers the "daily history" view showing each day's record.
     */
    @GetMapping
    public ResponseEntity<List<DailySnapshot>> getAll() {
        return ResponseEntity.ok(snapshotService.getAllOrderedByDate());
    }

    /**
     * GET /api/snapshots/{date}
     *
     * Returns the snapshot for a specific date.
     *
     * @param date the date to look up (yyyy-MM-dd)
     * @return the snapshot or 404 if no predictions were captured that day
     */
    @GetMapping("/{date}")
    public ResponseEntity<DailySnapshot> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return snapshotService.getByDate(date)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/snapshots/range?start=2026-03-01&end=2026-03-21
     *
     * Returns snapshots within a date range. Useful for weekly
     * or monthly rollup views on the frontend.
     *
     * @param start inclusive start date
     * @param end   inclusive end date
     */
    @GetMapping("/range")
    public ResponseEntity<List<DailySnapshot>> getByRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        return ResponseEntity.ok(snapshotService.getByDateRange(start, end));
    }

    /**
     * GET /api/snapshots/unresolved
     *
     * Returns snapshots that still have pending (unsettled) predictions.
     * Useful for seeing which days need outcome resolution.
     */
    @GetMapping("/unresolved")
    public ResponseEntity<List<DailySnapshot>> getUnresolved() {
        return ResponseEntity.ok(snapshotService.getUnresolved());
    }

    /**
     * GET /api/snapshots/aggregate
     *
     * Returns aggregate stats across all snapshots: total days tracked,
     * total picks, overall win rate, total profit, etc.
     */
    @GetMapping("/aggregate")
    public ResponseEntity<Map<String, Object>> getAggregateStats() {
        return ResponseEntity.ok(snapshotService.getAggregateStats());
    }

    /**
     * GET /api/snapshots/streak
     *
     * Returns the current streak: consecutive profitable or unprofitable days.
     * Response: { type: "WIN_STREAK" | "LOSS_STREAK", count: N }
     */
    @GetMapping("/streak")
    public ResponseEntity<Map<String, Object>> getStreak() {
        return ResponseEntity.ok(snapshotService.getCurrentStreak());
    }

    /**
     * POST /api/snapshots/{id}/recalculate
     *
     * Recalculates win/loss/push tallies for a specific snapshot
     * by re-examining all of its predictions. Use this if tallies
     * seem out of sync after manual adjustments.
     *
     * @param id the snapshot ID to recalculate
     */
    @PostMapping("/{id}/recalculate")
    public ResponseEntity<DailySnapshot> recalculate(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(snapshotService.recalculateStats(id));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
