package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.AccuracyReport;
import com.coltwarren.best_bets_tracker.model.enums.Sport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccuracyReportRepository extends JpaRepository<AccuracyReport, Long> {

    List<AccuracyReport> findByReportType(String reportType);

    List<AccuracyReport> findByReportTypeAndSport(String reportType, Sport sport);

    List<AccuracyReport> findByReportTypeOrderByPeriodStartDesc(String reportType);

    Optional<AccuracyReport> findByReportTypeAndPeriodStartAndPeriodEndAndSport(
            String reportType, LocalDate periodStart, LocalDate periodEnd, Sport sport);

    @Query("SELECT a FROM AccuracyReport a WHERE a.reportType = 'ALL_TIME' AND a.sport IS NULL " +
            "ORDER BY a.generatedAt DESC")
    Optional<AccuracyReport> findLatestAllTime();

    @Query("SELECT a FROM AccuracyReport a WHERE a.reportType = 'ALL_TIME' AND a.sport = :sport " +
            "ORDER BY a.generatedAt DESC")
    Optional<AccuracyReport> findLatestAllTimeBySport(@Param("sport") Sport sport);

    @Query("SELECT a FROM AccuracyReport a WHERE a.reportType = :type " +
            "ORDER BY a.periodStart DESC")
    List<AccuracyReport> findRecentByType(@Param("type") String type,
                                          org.springframework.data.domain.Pageable pageable);
}
