package com.coltwarren.best_bets_tracker.model.enums;

public enum BetResult {
    PENDING,
    WON,
    LOST,
    PUSH,
    CANCELLED;

    public boolean isSettled() {
        return this == WON || this == LOST || this == PUSH;
    }

    public boolean isWin() {
        return this == WON;
    }
}
