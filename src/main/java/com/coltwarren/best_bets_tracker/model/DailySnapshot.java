package com.coltwarren.best_bets_tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "daily_snapshots", indexes = {
        @Index(name = "idx_snapshot_date", columnList = "snapshotDate", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class DailySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private LocalDate snapshotDate;

    @Column(nullable = false)
    private int totalPicks;

    @Column(nullable = false)
    private int picksResolved;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int pushes;

    @Column(precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(precision = 10, scale = 2)
    private BigDecimal netProfitUnits;

    @OneToMany(mappedBy = "snapshot", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prediction> predictions = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === Business Methods ===

    /**
     * Recalculates win rate and profit from resolved predictions.
     */
    public void recalculateStats() {
        int settledCount = wins + losses;
        if (settledCount > 0) {
            winRate = BigDecimal.valueOf(wins)
                    .divide(BigDecimal.valueOf(settledCount), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            winRate = BigDecimal.ZERO;
        }
        picksResolved = wins + losses + pushes;
    }

    /**
     * Returns true if all picks for this day have been resolved.
     */
    public boolean isFullyResolved() {
        return picksResolved >= totalPicks && totalPicks > 0;
    }

    /**
     * Returns the number of picks still pending resolution.
     */
    public int getPendingCount() {
        return totalPicks - picksResolved;
    }
}
