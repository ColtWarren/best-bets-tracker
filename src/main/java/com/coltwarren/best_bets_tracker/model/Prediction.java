package com.coltwarren.best_bets_tracker.model;

import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.model.enums.BetType;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "predictions", indexes = {
        @Index(name = "idx_pred_sport", columnList = "sport"),
        @Index(name = "idx_pred_status", columnList = "status"),
        @Index(name = "idx_pred_event_start", columnList = "eventStartTime"),
        @Index(name = "idx_pred_snapshot", columnList = "snapshot_id"),
        @Index(name = "idx_pred_confidence", columnList = "confidence"),
        @Index(name = "idx_pred_sport_status", columnList = "sport, status")
})
@Getter
@Setter
@NoArgsConstructor
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "snapshot_id", nullable = false)
    private DailySnapshot snapshot;

    // === Game Info ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sport sport;

    @Column(nullable = false, length = 255)
    private String eventName;

    @Column(nullable = false, length = 150)
    private String homeTeam;

    @Column(nullable = false, length = 150)
    private String awayTeam;

    @Column(nullable = false)
    private LocalDateTime eventStartTime;

    // === AI Recommendation ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BetType betType;

    @Column(nullable = false, length = 200)
    private String selection;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal odds;

    @Column(nullable = false, precision = 4, scale = 1)
    private BigDecimal confidence;

    @Column(precision = 5, scale = 2)
    private BigDecimal kellyPercent;

    @Column(precision = 6, scale = 2)
    private BigDecimal expectedValue;

    @Column(columnDefinition = "TEXT")
    private String aiReasoning;

    // === Odds Source ===

    @Column(length = 100)
    private String sportsbook;

    @Column(precision = 8, scale = 2)
    private BigDecimal closingOdds;

    @Column
    private Boolean beatClosingLine;

    // === Status ===

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BetResult status = BetResult.PENDING;

    // === Timestamps ===

    @Column(nullable = false)
    private LocalDateTime predictionTime;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // === Relationship ===

    @OneToOne(mappedBy = "prediction", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Outcome outcome;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (predictionTime == null) {
            predictionTime = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Business Methods ===

    /**
     * Returns true if the game should have finished (start time + 4 hours).
     */
    public boolean isEligibleForSettlement() {
        return status == BetResult.PENDING
                && eventStartTime.plusHours(4).isBefore(LocalDateTime.now());
    }

    /**
     * Calculates profit/loss in units for a 1-unit flat bet.
     * Positive odds: profit = odds / 100
     * Negative odds: profit = 100 / |odds|
     */
    public BigDecimal calculateProfitUnits() {
        if (status == BetResult.WON) {
            if (odds.compareTo(BigDecimal.ZERO) > 0) {
                return odds.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
            } else {
                return BigDecimal.valueOf(100)
                        .divide(odds.abs(), 4, java.math.RoundingMode.HALF_UP);
            }
        } else if (status == BetResult.LOST) {
            return BigDecimal.ONE.negate();
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculates CLV: (yourDecimalOdds / closingDecimalOdds - 1) * 100
     */
    public BigDecimal calculateCLV() {
        if (closingOdds == null) {
            return null;
        }
        BigDecimal yourDecimal = americanToDecimal(odds);
        BigDecimal closingDecimal = americanToDecimal(closingOdds);
        if (closingDecimal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return yourDecimal
                .divide(closingDecimal, 6, java.math.RoundingMode.HALF_UP)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Converts American odds to decimal odds.
     */
    private BigDecimal americanToDecimal(BigDecimal american) {
        if (american.compareTo(BigDecimal.ZERO) > 0) {
            return american.divide(BigDecimal.valueOf(100), 6, java.math.RoundingMode.HALF_UP)
                    .add(BigDecimal.ONE);
        } else {
            return BigDecimal.valueOf(100)
                    .divide(american.abs(), 6, java.math.RoundingMode.HALF_UP)
                    .add(BigDecimal.ONE);
        }
    }
}
