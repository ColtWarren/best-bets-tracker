package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import com.coltwarren.best_bets_tracker.service.PredictionCaptureService;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for managing and querying AI best bet predictions.
 *
 * Capture endpoints trigger data ingestion from the main app.
 * Query endpoints provide filtered, paginated access to prediction data.
 */
@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final PredictionCaptureService captureService;
    private final PredictionRepository predictionRepository;

    public PredictionController(PredictionCaptureService captureService,
                                PredictionRepository predictionRepository) {
        this.captureService = captureService;
        this.predictionRepository = predictionRepository;
    }

    // =========================================================================
    // CAPTURE — ingest predictions from the main app
    // =========================================================================

    /**
     * POST /api/predictions/capture
     *
     * Triggers a capture of today's best bets from the main app.
     * Idempotent — safe to call multiple times; duplicates are skipped.
     *
     * @return the DailySnapshot with count of captured predictions
     */
    @PostMapping("/capture")
    public ResponseEntity<Map<String, Object>> captureToday() {
        try {
            DailySnapshot snapshot = captureService.captureToday();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", snapshot.getSnapshotDate());
            response.put("totalPicks", snapshot.getTotalPicks());
            response.put("message", "Captured " + snapshot.getTotalPicks() + " predictions");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * POST /api/predictions/capture/{date}
     *
     * Captures best bets for a specific date. Useful for backfilling
     * if the daily capture was missed. Note: the main app returns current
     * recommendations, so backfilling past dates is most accurate when
     * done on the actual day.
     *
     * @param date the date to associate with the capture (yyyy-MM-dd)
     */
    @PostMapping("/capture/{date}")
    public ResponseEntity<Map<String, Object>> captureForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            DailySnapshot snapshot = captureService.captureForDate(date);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("date", snapshot.getSnapshotDate());
            response.put("totalPicks", snapshot.getTotalPicks());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    // =========================================================================
    // QUERY — read prediction data
    // =========================================================================

    /**
     * GET /api/predictions
     *
     * Returns all predictions, optionally filtered by sport and/or status.
     *
     * @param sport  optional sport filter (e.g., NFL, NBA)
     * @param status optional status filter (e.g., PENDING, WON, LOST)
     */
    @GetMapping
    public ResponseEntity<List<Prediction>> getAll(
            @RequestParam(required = false) Sport sport,
            @RequestParam(required = false) BetResult status) {
        List<Prediction> predictions;

        if (sport != null && status != null) {
            predictions = predictionRepository.findBySportAndStatus(sport, status);
        } else if (sport != null) {
            predictions = predictionRepository.findBySport(sport);
        } else if (status != null) {
            predictions = predictionRepository.findByStatus(status);
        } else {
            predictions = predictionRepository.findAll();
        }

        return ResponseEntity.ok(predictions);
    }

    /**
     * GET /api/predictions/today
     *
     * Returns all predictions captured for today.
     */
    @GetMapping("/today")
    public ResponseEntity<List<Prediction>> getToday() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        List<Prediction> predictions = predictionRepository
                .findByCreatedAtBetween(startOfDay, endOfDay);
        return ResponseEntity.ok(predictions);
    }

    /**
     * GET /api/predictions/{id}
     *
     * Returns a single prediction by ID.
     *
     * @param id the prediction ID
     * @return the prediction or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prediction> getById(@PathVariable Long id) {
        return predictionRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/predictions/pending
     *
     * Returns all predictions that haven't been settled yet.
     * Useful for seeing what's still waiting for game results.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<Prediction>> getPending() {
        return ResponseEntity.ok(predictionRepository.findByStatus(BetResult.PENDING));
    }

    /**
     * GET /api/predictions/recent?limit=20
     *
     * Returns the most recently captured predictions.
     *
     * @param limit max number to return (default 20)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Prediction>> getRecent(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(
                predictionRepository.findRecent(PageRequest.of(0, limit)));
    }
}
