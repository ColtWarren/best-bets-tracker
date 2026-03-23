package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.SimulatedBet;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SimulatedBetRepository extends JpaRepository<SimulatedBet, Long> {

    List<SimulatedBet> findByStatus(BetResult status);

    List<SimulatedBet> findByPredictionId(Long predictionId);

    List<SimulatedBet> findBySportsbookId(Long sportsbookId);

    // === Bankroll Aggregates ===

    @Query("SELECT COALESCE(SUM(s.profitLoss), 0) FROM SimulatedBet s WHERE s.profitLoss IS NOT NULL")
    BigDecimal calculateTotalProfitLoss();

    @Query("SELECT COALESCE(SUM(s.stake), 0) FROM SimulatedBet s")
    BigDecimal calculateTotalStaked();

    @Query("SELECT COALESCE(SUM(s.profitLoss), 0) FROM SimulatedBet s " +
            "WHERE s.sportsbook.id = :sportsbookId AND s.profitLoss IS NOT NULL")
    BigDecimal calculateProfitLossBySportsbook(@org.springframework.data.repository.query.Param("sportsbookId") Long sportsbookId);

    // === Counts ===

    long countByStatus(BetResult status);
}
