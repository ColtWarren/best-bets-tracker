package com.coltwarren.best_bets_tracker.model;

import com.coltwarren.best_bets_tracker.model.enums.Sport;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "accuracy_reports", indexes = {
        @Index(name = "idx_report_type_period", columnList = "reportType, periodStart, periodEnd"),
        @Index(name = "idx_report_sport", columnList = "sport"),
        @Index(name = "idx_report_generated", columnList = "generatedAt")
})
@Getter
@Setter
@NoArgsConstructor
public class AccuracyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String reportType;  // DAILY, WEEKLY, MONTHLY, SPORT, ALL_TIME

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sport sport;  // null = all sports

    @Column(length = 50)
    private String betType;  // null = all types

    // === Stats ===

    @Column(nullable = false)
    private int totalPicks;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(nullable = false)
    private int pushes;

    @Column(precision = 5, scale = 2)
    private BigDecimal winRate;

    @Column(precision = 6, scale = 2)
    private BigDecimal roiPercent;

    @Column(precision = 10, scale = 2)
    private BigDecimal netUnits;

    @Column(precision = 8, scale = 2)
    private BigDecimal avgOdds;

    @Column(precision = 4, scale = 1)
    private BigDecimal avgConfidence;

    @Column(precision = 5, scale = 2)
    private BigDecimal clvRate;  // % of picks that beat closing line

    @Column(nullable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    // === Business Methods ===

    public boolean isProfitable() {
        return netUnits != null && netUnits.compareTo(BigDecimal.ZERO) > 0;
    }

    public int getSettledCount() {
        return wins + losses;
    }
}
