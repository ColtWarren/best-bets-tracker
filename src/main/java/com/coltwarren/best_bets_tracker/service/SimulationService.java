package com.coltwarren.best_bets_tracker.service;

import com.coltwarren.best_bets_tracker.model.MissouriSportsbook;
import com.coltwarren.best_bets_tracker.model.Prediction;
import com.coltwarren.best_bets_tracker.model.SimulatedBet;
import com.coltwarren.best_bets_tracker.model.enums.BetResult;
import com.coltwarren.best_bets_tracker.repository.MissouriSportsbookRepository;
import com.coltwarren.best_bets_tracker.repository.PredictionRepository;
import com.coltwarren.best_bets_tracker.repository.SimulatedBetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Simulates a betting bankroll to answer: "If I had bet real money on
 * every AI recommendation, how much would I have made or lost?"
 *
 * Supports two staking strategies:
 * 1. FLAT — bet a fixed amount on every pick (e.g., $50/bet)
 * 2. KELLY — use the AI's Kelly % recommendation to size bets proportionally
 *
 * The simulation creates SimulatedBet records linked to each Prediction.
 * When predictions are settled, the simulated bets are settled too.
 *
 * This lets you evaluate the AI's profitability without risking real money.
 */
@Service
public class SimulationService {

    private static final Logger log = LoggerFactory.getLogger(SimulationService.class);

    private final SimulatedBetRepository simulatedBetRepository;
    private final PredictionRepository predictionRepository;
    private final MissouriSportsbookRepository sportsbookRepository;

    public SimulationService(SimulatedBetRepository simulatedBetRepository,
                             PredictionRepository predictionRepository,
                             MissouriSportsbookRepository sportsbookRepository) {
        this.simulatedBetRepository = simulatedBetRepository;
        this.predictionRepository = predictionRepository;
        this.sportsbookRepository = sportsbookRepository;
    }

    /**
     * Creates a simulated flat bet for a prediction.
     * Flat betting means every bet is the same dollar amount regardless
     * of confidence or odds.
     *
     * @param prediction the AI recommendation to simulate betting on
     * @param flatStake  the fixed amount to "wager" (e.g., $50)
     * @return the created SimulatedBet
     */
    @Transactional
    public SimulatedBet createFlatBet(Prediction prediction, BigDecimal flatStake) {
        // Avoid duplicate simulated bets for the same prediction
        List<SimulatedBet> existing = simulatedBetRepository.findByPredictionId(prediction.getId());
        if (!existing.isEmpty()) {
            log.debug("Simulated bet already exists for prediction {}", prediction.getId());
            return existing.get(0);
        }

        SimulatedBet bet = new SimulatedBet();
        bet.setPrediction(prediction);
        bet.setStake(flatStake);
        bet.setStatus(BetResult.PENDING);

        // Link to the Missouri sportsbook the AI recommended
        linkSportsbook(bet, prediction.getSportsbook());

        log.debug("Created flat simulated bet: ${} on {}",
                flatStake, prediction.getEventName());

        return simulatedBetRepository.save(bet);
    }

    /**
     * Creates a simulated Kelly bet for a prediction.
     * Kelly betting sizes each bet based on the AI's kelly percentage
     * and the current simulated bankroll — bigger bets on higher-confidence picks.
     *
     * @param prediction the AI recommendation to simulate betting on
     * @param bankroll   the current simulated bankroll to calculate stake from
     * @return the created SimulatedBet
     */
    @Transactional
    public SimulatedBet createKellyBet(Prediction prediction, BigDecimal bankroll) {
        // Avoid duplicates
        List<SimulatedBet> existing = simulatedBetRepository.findByPredictionId(prediction.getId());
        if (!existing.isEmpty()) {
            return existing.get(0);
        }

        // Calculate stake: bankroll * (kellyPercent / 100)
        BigDecimal kellyPercent = prediction.getKellyPercent();
        if (kellyPercent == null || kellyPercent.compareTo(BigDecimal.ZERO) <= 0) {
            // If no Kelly %, use a conservative 1% of bankroll
            kellyPercent = BigDecimal.ONE;
        }

        BigDecimal stake = bankroll
                .multiply(kellyPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Minimum bet of $1 to keep things sensible
        if (stake.compareTo(BigDecimal.ONE) < 0) {
            stake = BigDecimal.ONE;
        }

        SimulatedBet bet = new SimulatedBet();
        bet.setPrediction(prediction);
        bet.setStake(stake);
        bet.setStatus(BetResult.PENDING);

        linkSportsbook(bet, prediction.getSportsbook());

        log.debug("Created Kelly simulated bet: ${} ({}% of ${}) on {}",
                stake, kellyPercent, bankroll, prediction.getEventName());

        return simulatedBetRepository.save(bet);
    }

    /**
     * Creates simulated bets for ALL pending predictions that don't have one yet.
     * Call this after PredictionCaptureService runs to auto-generate simulated bets.
     *
     * @param flatStake the fixed stake for flat betting (null to use Kelly)
     * @param bankroll  the current bankroll for Kelly sizing
     * @return number of simulated bets created
     */
    @Transactional
    public int createBetsForAllPending(BigDecimal flatStake, BigDecimal bankroll) {
        List<Prediction> pending = predictionRepository.findByStatus(BetResult.PENDING);
        int created = 0;

        for (Prediction prediction : pending) {
            // Skip if already has a simulated bet
            if (!simulatedBetRepository.findByPredictionId(prediction.getId()).isEmpty()) {
                continue;
            }

            if (flatStake != null) {
                createFlatBet(prediction, flatStake);
            } else {
                createKellyBet(prediction, bankroll);
            }
            created++;
        }

        log.info("Created {} simulated bets for pending predictions", created);
        return created;
    }

    /**
     * Settles all pending simulated bets whose predictions have been resolved.
     * Called after OutcomeResolutionService settles predictions.
     *
     * @return number of simulated bets settled
     */
    @Transactional
    public int settleResolvedBets() {
        List<SimulatedBet> pendingBets = simulatedBetRepository.findByStatus(BetResult.PENDING);
        int settled = 0;

        for (SimulatedBet bet : pendingBets) {
            Prediction prediction = bet.getPrediction();
            if (prediction.getStatus().isSettled()) {
                bet.settle(prediction.getStatus());
                simulatedBetRepository.save(bet);
                settled++;
            }
        }

        log.info("Settled {} simulated bets", settled);
        return settled;
    }

    /**
     * Returns the current simulated bankroll: starting amount + total P/L.
     *
     * @param startingBankroll the initial bankroll (e.g., $1000)
     * @return current simulated bankroll balance
     */
    public BigDecimal getCurrentBankroll(BigDecimal startingBankroll) {
        BigDecimal totalPL = simulatedBetRepository.calculateTotalProfitLoss();
        return startingBankroll.add(totalPL);
    }

    /**
     * Returns a full bankroll summary showing starting balance, current balance,
     * total P/L, ROI, and bet counts.
     *
     * @param startingBankroll the initial bankroll to simulate from
     * @return comprehensive bankroll stats
     */
    public Map<String, Object> getBankrollSummary(BigDecimal startingBankroll) {
        BigDecimal totalPL = simulatedBetRepository.calculateTotalProfitLoss();
        BigDecimal totalStaked = simulatedBetRepository.calculateTotalStaked();
        BigDecimal currentBankroll = startingBankroll.add(totalPL);

        // ROI: total P/L divided by total staked
        BigDecimal roi = BigDecimal.ZERO;
        if (totalStaked.compareTo(BigDecimal.ZERO) > 0) {
            roi = totalPL
                    .divide(totalStaked, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Growth: (current - starting) / starting * 100
        BigDecimal growth = BigDecimal.ZERO;
        if (startingBankroll.compareTo(BigDecimal.ZERO) > 0) {
            growth = currentBankroll.subtract(startingBankroll)
                    .divide(startingBankroll, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        long totalBets = simulatedBetRepository.count();
        long wonBets = simulatedBetRepository.countByStatus(BetResult.WON);
        long lostBets = simulatedBetRepository.countByStatus(BetResult.LOST);
        long pendingBets = simulatedBetRepository.countByStatus(BetResult.PENDING);

        Map<String, Object> summary = new HashMap<>();
        summary.put("startingBankroll", startingBankroll);
        summary.put("currentBankroll", currentBankroll);
        summary.put("totalProfitLoss", totalPL);
        summary.put("totalStaked", totalStaked);
        summary.put("roi", roi);
        summary.put("growth", growth);
        summary.put("totalBets", totalBets);
        summary.put("wonBets", wonBets);
        summary.put("lostBets", lostBets);
        summary.put("pendingBets", pendingBets);

        return summary;
    }

    /**
     * Returns P/L breakdown by Missouri sportsbook.
     * Shows which book would have been most profitable to bet at.
     *
     * @return list of maps with {sportsbook, profitLoss, betCount}
     */
    public List<Map<String, Object>> getProfitBySportsbook() {
        List<Map<String, Object>> results = new ArrayList<>();

        for (Object[] row : simulatedBetRepository.getProfitBySportsbookAggregated()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("sportsbook", row[0]);
            entry.put("profitLoss", row[1]);
            entry.put("betCount", row[2]);
            results.add(entry);
        }

        return results;
    }

    // === Helpers ===

    /**
     * Links a simulated bet to the matching Missouri sportsbook entity.
     * Matches by name from the prediction's recommended sportsbook.
     */
    private void linkSportsbook(SimulatedBet bet, String sportsbookName) {
        if (sportsbookName == null) return;
        sportsbookRepository.findByName(sportsbookName)
                .ifPresent(bet::setSportsbook);
    }
}
