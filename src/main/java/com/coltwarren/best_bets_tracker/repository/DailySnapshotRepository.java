package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.DailySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailySnapshotRepository extends JpaRepository<DailySnapshot, Long> {

    Optional<DailySnapshot> findBySnapshotDate(LocalDate date);

    boolean existsBySnapshotDate(LocalDate date);

    List<DailySnapshot> findBySnapshotDateBetween(LocalDate start, LocalDate end);

    @Query("SELECT d FROM DailySnapshot d ORDER BY d.snapshotDate DESC")
    List<DailySnapshot> findAllOrderByDateDesc();

    // === Aggregate Stats ===

    @Query("SELECT COALESCE(SUM(d.wins), 0) FROM DailySnapshot d")
    long sumAllWins();

    @Query("SELECT COALESCE(SUM(d.losses), 0) FROM DailySnapshot d")
    long sumAllLosses();

    @Query("SELECT COALESCE(SUM(d.netProfitUnits), 0) FROM DailySnapshot d")
    BigDecimal sumAllProfitUnits();

    @Query("SELECT COALESCE(SUM(d.totalPicks), 0) FROM DailySnapshot d")
    long sumAllPicks();

    // === Streak Queries ===

    @Query("SELECT d FROM DailySnapshot d WHERE d.picksResolved > 0 ORDER BY d.snapshotDate DESC")
    List<DailySnapshot> findResolvedDaysDesc();

    // === Unresolved ===

    @Query("SELECT d FROM DailySnapshot d WHERE d.picksResolved < d.totalPicks AND d.snapshotDate < :today ORDER BY d.snapshotDate ASC")
    List<DailySnapshot> findUnresolvedBefore(@Param("today") LocalDate today);
}
