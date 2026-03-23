package com.coltwarren.best_bets_tracker.model.enums;

public enum BetType {
    MONEYLINE,
    SPREAD,
    TOTAL_OVER,
    TOTAL_UNDER,
    HOME_WIN,
    DRAW,
    AWAY_WIN,
    PROP;

    /**
     * Returns true if this is a soccer 3-way bet type.
     */
    public boolean isSoccerType() {
        return this == HOME_WIN || this == DRAW || this == AWAY_WIN;
    }

    /**
     * Returns true if this is a totals bet.
     */
    public boolean isTotalsBet() {
        return this == TOTAL_OVER || this == TOTAL_UNDER;
    }
}
