package com.coltwarren.best_bets_tracker.model;

import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulated_bets", indexes = {
        @Index(name = "idx_sim_prediction", columnList = "prediction_id"),
        @Index(name = "idx_sim_sportsbook", columnList = "sportsbook_id"),
        @Index(name = "idx_sim_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
public class SimulatedBet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    private Prediction prediction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sportsbook_id")
    private MissouriSportsbook sportsbook;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal stake;

    @Column(precision = 10, scale = 2)
    private BigDecimal potentialPayout;

    @Column(precision = 10, scale = 2)
    private BigDecimal actualPayout;

    @Column(precision = 10, scale = 2)
    private BigDecimal profitLoss;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetResult status = BetResult.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (potentialPayout == null) {
            potentialPayout = calculatePotentialPayout();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Business Methods ===

    /**
     * Calculates potential payout from stake and prediction odds (American format).
     */
    public BigDecimal calculatePotentialPayout() {
        if (stake == null || prediction == null || prediction.getOdds() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal odds = prediction.getOdds();
        if (odds.compareTo(BigDecimal.ZERO) > 0) {
            // Positive odds: payout = stake * (odds / 100) + stake
            return stake.multiply(odds)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .add(stake);
        } else {
            // Negative odds: payout = stake * (100 / |odds|) + stake
            return stake.multiply(BigDecimal.valueOf(100))
                    .divide(odds.abs(), 2, RoundingMode.HALF_UP)
                    .add(stake);
        }
    }

    /**
     * Settles this simulated bet based on the prediction outcome.
     */
    public void settle(BetResult result) {
        this.status = result;
        switch (result) {
            case WON -> {
                this.actualPayout = calculatePotentialPayout();
                this.profitLoss = actualPayout.subtract(stake);
            }
            case LOST -> {
                this.actualPayout = BigDecimal.ZERO;
                this.profitLoss = stake.negate();
            }
            case PUSH -> {
                this.actualPayout = stake;
                this.profitLoss = BigDecimal.ZERO;
            }
            default -> {
                this.actualPayout = null;
                this.profitLoss = null;
            }
        }
    }
}
