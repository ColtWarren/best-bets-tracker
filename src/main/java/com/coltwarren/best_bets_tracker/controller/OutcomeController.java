package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.model.Outcome;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.repository.OutcomeRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import com.coltwarren.best_bets_tracker.service.OutcomeResolutionService;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST endpoints for outcome resolution and querying settled results.
 *
 * The resolve endpoint triggers ESPN score checks for pending predictions.
 * Query endpoints provide access to settled outcome data.
 */
@RestController
@RequestMapping("/api/outcomes")
public class OutcomeController {

    private final OutcomeResolutionService resolutionService;
    private final OutcomeRepository outcomeRepository;
    private final PredictionRepository predictionRepository;

    public OutcomeController(OutcomeResolutionService resolutionService,
                             OutcomeRepository outcomeRepository,
                             PredictionRepository predictionRepository) {
        this.resolutionService = resolutionService;
        this.outcomeRepository = outcomeRepository;
        this.predictionRepository = predictionRepository;
    }

    /**
     * POST /api/outcomes/resolve
     *
     * Triggers settlement of all eligible pending predictions.
     * Checks ESPN for final scores on games that started 4+ hours ago.
     * Safe to call repeatedly — already-settled predictions are skipped.
     *
     * @return summary with counts of settled, failed, and still-pending
     */
    @PostMapping("/resolve")
    public ResponseEntity<Map<String, Object>> resolveAll() {
        try {
            Map<String, Object> result = resolutionService.resolveAll();
            result.put("success", true);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * POST /api/outcomes/resolve/{predictionId}
     *
     * Attempts to resolve a single prediction by its ID.
     * Useful for manually triggering settlement of a specific pick.
     *
     * @param predictionId the prediction to resolve
     */
    @PostMapping("/resolve/{predictionId}")
    public ResponseEntity<Map<String, Object>> resolveOne(@PathVariable Long predictionId) {
        try {
            Prediction prediction = predictionRepository.findById(predictionId)
                    .orElse(null);
            if (prediction == null) {
                return ResponseEntity.notFound().build();
            }
            BetResult result = resolutionService.resolveOne(prediction);
            return ResponseEntity.ok(Map.of("success", true, "result",
                    result != null ? result.name() : "GAME_NOT_FINAL"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * GET /api/outcomes
     *
     * Returns all outcomes (settled results).
     */
    @GetMapping
    public ResponseEntity<List<Outcome>> getAll() {
        return ResponseEntity.ok(outcomeRepository.findAll());
    }

    /**
     * GET /api/outcomes/{id}
     *
     * Returns a single outcome by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Outcome> getById(@PathVariable Long id) {
        return outcomeRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/outcomes/recent?limit=20
     *
     * Returns the most recently settled outcomes with their prediction data.
     * Uses JOIN FETCH to avoid N+1 queries.
     *
     * @param limit max number to return (default 20)
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Outcome>> getRecent(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(
                outcomeRepository.findRecentWithPrediction(PageRequest.of(0, limit)));
    }

    /**
     * GET /api/outcomes/prediction/{predictionId}
     *
     * Returns the outcome for a specific prediction.
     *
     * @param predictionId the prediction ID to look up
     * @return the outcome or 404 if not yet resolved
     */
    @GetMapping("/prediction/{predictionId}")
    public ResponseEntity<Outcome> getByPrediction(@PathVariable Long predictionId) {
        return outcomeRepository.findByPredictionId(predictionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
