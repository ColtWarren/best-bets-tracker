package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.model.enums.BetType;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PredictionRepository extends JpaRepository<Prediction, Long> {

    // === Status Queries ===

    List<Prediction> findByStatus(BetResult status);

    List<Prediction> findByStatusIn(List<BetResult> statuses);

    long countByStatus(BetResult status);

    // === Filtering ===

    List<Prediction> findBySport(Sport sport);

    List<Prediction> findBySportAndStatus(Sport sport, BetResult status);

    List<Prediction> findByBetType(BetType betType);

    List<Prediction> findByBetTypeAndStatus(BetType betType, BetResult status);

    List<Prediction> findBySnapshotId(Long snapshotId);

    // === Time-Based ===

    List<Prediction> findByEventStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Prediction> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT p FROM Prediction p WHERE p.status = 'PENDING' AND p.eventStartTime < :cutoff")
    List<Prediction> findSettlementEligible(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT p FROM Prediction p ORDER BY p.createdAt DESC")
    List<Prediction> findRecent(Pageable pageable);

    // === Accuracy Aggregates ===

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.status = 'WON'")
    long countWins();

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.status = 'LOST'")
    long countLosses();

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.status IN ('WON', 'LOST')")
    long countSettled();

    @Query("SELECT CAST(COUNT(CASE WHEN p.status = 'WON' THEN 1 END) AS double) " +
            "/ NULLIF(COUNT(CASE WHEN p.status IN ('WON', 'LOST') THEN 1 END), 0) " +
            "FROM Prediction p")
    Double calculateOverallWinRate();

    // === Accuracy by Sport ===

    @Query("SELECT CAST(COUNT(CASE WHEN p.status = 'WON' THEN 1 END) AS double) " +
            "/ NULLIF(COUNT(CASE WHEN p.status IN ('WON', 'LOST') THEN 1 END), 0) " +
            "FROM Prediction p WHERE p.sport = :sport")
    Double calculateWinRateBySport(@Param("sport") Sport sport);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.sport = :sport AND p.status = 'WON'")
    long countWinsBySport(@Param("sport") Sport sport);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.sport = :sport AND p.status = 'LOST'")
    long countLossesBySport(@Param("sport") Sport sport);

    // === Accuracy by Bet Type ===

    @Query("SELECT CAST(COUNT(CASE WHEN p.status = 'WON' THEN 1 END) AS double) " +
            "/ NULLIF(COUNT(CASE WHEN p.status IN ('WON', 'LOST') THEN 1 END), 0) " +
            "FROM Prediction p WHERE p.betType = :betType")
    Double calculateWinRateByBetType(@Param("betType") BetType betType);

    // === Accuracy by Confidence Tier ===

    @Query("SELECT CAST(COUNT(CASE WHEN p.status = 'WON' THEN 1 END) AS double) " +
            "/ NULLIF(COUNT(CASE WHEN p.status IN ('WON', 'LOST') THEN 1 END), 0) " +
            "FROM Prediction p WHERE p.confidence >= :minConf AND p.confidence < :maxConf")
    Double calculateWinRateByConfidenceRange(
            @Param("minConf") BigDecimal minConf,
            @Param("maxConf") BigDecimal maxConf);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.confidence >= :minConf AND p.confidence < :maxConf AND p.status IN ('WON', 'LOST')")
    long countSettledByConfidenceRange(
            @Param("minConf") BigDecimal minConf,
            @Param("maxConf") BigDecimal maxConf);

    // === CLV ===

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.beatClosingLine = true")
    long countBeatClosingLine();

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.closingOdds IS NOT NULL")
    long countWithClosingOdds();

    // === Averages ===

    @Query("SELECT AVG(p.odds) FROM Prediction p WHERE p.status IN ('WON', 'LOST')")
    Double calculateAvgOdds();

    @Query("SELECT AVG(p.confidence) FROM Prediction p")
    Double calculateAvgConfidence();

    @Query("SELECT AVG(p.confidence) FROM Prediction p WHERE p.sport = :sport")
    Double calculateAvgConfidenceBySport(@Param("sport") Sport sport);

    // === Time-Scoped Aggregates ===

    @Query("SELECT CAST(COUNT(CASE WHEN p.status = 'WON' THEN 1 END) AS double) " +
            "/ NULLIF(COUNT(CASE WHEN p.status IN ('WON', 'LOST') THEN 1 END), 0) " +
            "FROM Prediction p WHERE p.predictionTime >= :since")
    Double calculateWinRateSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.status = 'WON' AND p.predictionTime >= :since")
    long countWinsSince(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(p) FROM Prediction p WHERE p.status = 'LOST' AND p.predictionTime >= :since")
    long countLossesSince(@Param("since") LocalDateTime since);
}
