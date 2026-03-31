package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.Outcome;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OutcomeRepository extends JpaRepository<Outcome, Long> {

    Optional<Outcome> findByPredictionId(Long predictionId);

    List<Outcome> findByBetResult(BetResult result);

    List<Outcome> findBySettledAtBetween(LocalDateTime start, LocalDateTime end);

    // === Profit Aggregates ===

    @Query("SELECT COALESCE(SUM(o.profitUnits), 0) FROM Outcome o")
    BigDecimal calculateTotalProfitUnits();

    @Query("SELECT COALESCE(SUM(o.profitUnits), 0) FROM Outcome o " +
            "WHERE o.prediction.sport = :sport")
    BigDecimal calculateProfitUnitsBySport(@Param("sport") com.coltwarren.best_bets_tracker.model.enums.Sport sport);

    @Query("SELECT COALESCE(SUM(o.profitUnits), 0) FROM Outcome o " +
            "WHERE o.settledAt >= :since")
    BigDecimal calculateProfitUnitsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(o.profitUnits), 0) FROM Outcome o " +
            "WHERE o.prediction.eventStartTime >= :start AND o.prediction.eventStartTime < :end")
    BigDecimal calculateProfitUnitsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // === CLV Aggregates ===

    @Query("SELECT COUNT(o) FROM Outcome o WHERE o.beatClosingLine = true")
    long countBeatClosingLine();

    @Query("SELECT COUNT(o) FROM Outcome o WHERE o.closingOdds IS NOT NULL")
    long countWithClosingOdds();

    // === Recent Outcomes ===

    @Query("SELECT o FROM Outcome o JOIN FETCH o.prediction ORDER BY o.settledAt DESC")
    List<Outcome> findRecentWithPrediction(org.springframework.data.domain.Pageable pageable);
}
