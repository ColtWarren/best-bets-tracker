package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.service.SimulationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * REST endpoints for the simulated bankroll tracker.
 *
 * Simulates what would happen if you bet real money on every
 * AI recommendation. Supports flat betting (fixed stake) and
 * Kelly betting (proportional to confidence/edge).
 *
 * No real money is involved — this is purely analytical.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    /**
     * POST /api/simulation/create-bets
     *
     * Creates simulated bets for all pending predictions that don't
     * already have one. Call this after capturing new predictions.
     *
     * @param strategy  "flat" or "kelly" (default flat)
     * @param stake     flat stake amount in dollars (default $50, used for flat strategy)
     * @param bankroll  starting bankroll for Kelly sizing (default $1000)
     * @return number of simulated bets created
     */
    @PostMapping("/create-bets")
    public ResponseEntity<Map<String, Object>> createBets(
            @RequestParam(defaultValue = "flat") String strategy,
            @RequestParam(defaultValue = "50") BigDecimal stake,
            @RequestParam(defaultValue = "1000") BigDecimal bankroll) {
        int created;

        if ("kelly".equalsIgnoreCase(strategy)) {
            // Kelly sizing: use current simulated bankroll
            BigDecimal currentBankroll = simulationService.getCurrentBankroll(bankroll);
            created = simulationService.createBetsForAllPending(null, currentBankroll);
        } else {
            // Flat betting: fixed stake per bet
            created = simulationService.createBetsForAllPending(stake, bankroll);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "strategy", strategy,
                "betsCreated", created
        ));
    }

    /**
     * POST /api/simulation/settle
     *
     * Settles all simulated bets whose predictions have been resolved.
     * Call this after running outcome resolution.
     *
     * @return number of simulated bets settled
     */
    @PostMapping("/settle")
    public ResponseEntity<Map<String, Object>> settleBets() {
        int settled = simulationService.settleResolvedBets();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "settled", settled
        ));
    }

    /**
     * GET /api/simulation/bankroll?startingBankroll=1000
     *
     * Returns the current simulated bankroll summary: starting balance,
     * current balance, total P/L, ROI, growth percentage, and bet counts.
     *
     * @param startingBankroll the initial bankroll to simulate from (default $1000)
     */
    @GetMapping("/bankroll")
    public ResponseEntity<Map<String, Object>> getBankroll(
            @RequestParam(defaultValue = "1000") BigDecimal startingBankroll) {
        return ResponseEntity.ok(simulationService.getBankrollSummary(startingBankroll));
    }

    /**
     * GET /api/simulation/by-sportsbook
     *
     * Returns profit/loss breakdown by Missouri sportsbook.
     * Shows which book would have been most profitable to bet at.
     *
     * Response: [{ sportsbook, profitLoss, betCount }, ...]
     */
    @GetMapping("/by-sportsbook")
    public ResponseEntity<List<Map<String, Object>>> getProfitBySportsbook() {
        return ResponseEntity.ok(simulationService.getProfitBySportsbook());
    }
}
