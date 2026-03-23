package com.coltwarren.best_bets_tracker.repository;

import com.coltwarren.best_bets_tracker.model.MissouriSportsbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MissouriSportsbookRepository extends JpaRepository<MissouriSportsbook, Long> {

    Optional<MissouriSportsbook> findByName(String name);

    List<MissouriSportsbook> findByActiveTrue();

    boolean existsByName(String name);
}
