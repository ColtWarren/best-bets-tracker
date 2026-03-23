package com.coltwarren.best_bets_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BestBetsTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(BestBetsTrackerApplication.class, args);
    }
}
