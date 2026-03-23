package com.coltwarren.best_bets_tracker.config;

import com.coltwarren.best_bets_tracker.model.MissouriSportsbook;
import com.coltwarren.best_bets_tracker.repository.MissouriSportsbookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds the database with Missouri's 8 licensed sportsbooks on first startup.
 * Skips any that already exist (safe to run repeatedly).
 *
 * Missouri legalized sports betting in late 2023. Only bets placed
 * through these licensed operators are legal in the state.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final MissouriSportsbookRepository sportsbookRepository;

    public DataSeeder(MissouriSportsbookRepository sportsbookRepository) {
        this.sportsbookRepository = sportsbookRepository;
    }

    @Override
    public void run(String... args) {
        seedSportsbooks();
    }

    private void seedSportsbooks() {
        Object[][] books = {
                {"DraftKings",           true,  "Bet $5, Get $200 in bonus bets"},
                {"FanDuel",              true,  "Bet $5, Get $150 in bonus bets"},
                {"BetMGM",               true,  "Up to $1,500 back in bonus bets"},
                {"Caesars Sportsbook",   true,  "Up to $1,000 first bet on Caesars"},
                {"bet365",               true,  "Bet $1, Get $200 in bonus bets"},
                {"Fanatics Sportsbook",  true,  "Get up to $1,000 in bonus bets"},
                {"Circa Sports",         true,  null},
                {"theScore Bet",         true,  null},
        };

        int seeded = 0;
        for (Object[] book : books) {
            String name = (String) book[0];
            if (!sportsbookRepository.existsByName(name)) {
                MissouriSportsbook sb = new MissouriSportsbook(
                        name,
                        (boolean) book[1],
                        (String) book[2]
                );
                sportsbookRepository.save(sb);
                seeded++;
            }
        }

        if (seeded > 0) {
            log.info("Seeded {} Missouri sportsbooks", seeded);
        } else {
            log.debug("All Missouri sportsbooks already exist");
        }
    }
}
