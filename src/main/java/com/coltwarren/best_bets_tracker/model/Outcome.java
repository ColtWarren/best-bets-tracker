package com.coltwarren.best_bets_tracker.model;

import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "outcomes", indexes = {
        @Index(name = "idx_outcome_prediction", columnList = "prediction_id", unique = true),
        @Index(name = "idx_outcome_result", columnList = "betResult"),
        @Index(name = "idx_outcome_settled", columnList = "settledAt")
})
@Getter
@Setter
@NoArgsConstructor
public class Outcome {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false, unique = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "outcome", "snapshot"})
    private Prediction prediction;

    // === Game Result ===

    @Column(length = 255)
    private String actualResult;

    @Column
    private Integer finalScoreHome;

    @Column
    private Integer finalScoreAway;

    // === Bet Outcome ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetResult betResult;

    @Column(precision = 8, scale = 2)
    private BigDecimal closingOdds;

    @Column
    private Boolean beatClosingLine;

    /**
     * Profit/loss in units based on a 1-unit flat bet.
     * WON: positive (based on odds), LOST: -1.00, PUSH: 0.00
     */
    @Column(precision = 8, scale = 2)
    private BigDecimal profitUnits;

    // === Meta ===

    @Column(nullable = false)
    private LocalDateTime settledAt;

    @Column(length = 50)
    private String source;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (settledAt == null) {
            settledAt = LocalDateTime.now();
        }
    }

    // === Business Methods ===

    /**
     * Populates the outcome fields from a prediction and game score.
     * Call this after setting finalScoreHome, finalScoreAway, and betResult.
     */
    public void settle(Prediction prediction, BetResult result) {
        this.prediction = prediction;
        this.betResult = result;
        this.settledAt = LocalDateTime.now();
        this.actualResult = buildResultString();

        // Set prediction status BEFORE calculating profit (it checks status)
        prediction.setStatus(result);
        this.profitUnits = prediction.calculateProfitUnits();

        // CLV tracking
        if (prediction.getClosingOdds() != null) {
            this.closingOdds = prediction.getClosingOdds();
            this.beatClosingLine = prediction.calculateCLV() != null
                    && prediction.calculateCLV().compareTo(BigDecimal.ZERO) > 0;
            prediction.setBeatClosingLine(this.beatClosingLine);
        }
    }

    private String buildResultString() {
        if (finalScoreHome != null && finalScoreAway != null && prediction != null) {
            return prediction.getHomeTeam() + " " + finalScoreHome
                    + " - " + prediction.getAwayTeam() + " " + finalScoreAway;
        }
        return null;
    }
}
