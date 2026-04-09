package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.service.MlbAssessmentCaptureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint for manually triggering MLB assessment capture.
 * Used from the dashboard when you want to pull assessments on-demand
 * rather than waiting for the 11 AM scheduled job.
 */
@RestController
@RequestMapping("/api/mlb")
public class MlbAssessmentController {

    private static final Logger log = LoggerFactory.getLogger(MlbAssessmentController.class);

    private final MlbAssessmentCaptureService mlbAssessmentCaptureService;

    public MlbAssessmentController(MlbAssessmentCaptureService mlbAssessmentCaptureService) {
        this.mlbAssessmentCaptureService = mlbAssessmentCaptureService;
    }

    /**
     * Manually triggers MLB assessment capture for today.
     *
     * @return summary with saved and skipped counts
     */
    @PostMapping("/capture")
    public ResponseEntity<Map<String, Object>> captureAssessments() {
        try {
            var result = mlbAssessmentCaptureService.captureToday();
            log.info("Manual MLB capture: {} saved, {} skipped", result.saved(), result.skipped());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "saved", result.saved(),
                    "skipped", result.skipped(),
                    "message", result.saved() + " assessments captured, " + result.skipped() + " duplicates skipped"
            ));
        } catch (Exception e) {
            log.error("Manual MLB capture failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
