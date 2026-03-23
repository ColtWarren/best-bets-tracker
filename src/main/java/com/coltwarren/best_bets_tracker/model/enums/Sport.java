package com.coltwarren.best_bets_tracker.model.enums;

public enum Sport {
    NFL("americanfootball_nfl", "Football"),
    CFB("americanfootball_ncaaf", "Football"),
    NBA("basketball_nba", "Basketball"),
    WNBA("basketball_wnba", "Basketball"),
    CBB("basketball_ncaab", "Basketball"),
    WCBB("basketball_wncaab", "Basketball"),
    MLB("baseball_mlb", "Baseball"),
    NHL("icehockey_nhl", "Hockey"),
    SOCCER("soccer_epl", "Soccer");

    private final String oddsApiKey;
    private final String category;

    Sport(String oddsApiKey, String category) {
        this.oddsApiKey = oddsApiKey;
        this.category = category;
    }

    public String getOddsApiKey() {
        return oddsApiKey;
    }

    public String getCategory() {
        return category;
    }

    /**
     * Returns true if this sport is played outdoors (weather matters).
     */
    public boolean isOutdoorSport() {
        return this == NFL || this == CFB || this == MLB || this == SOCCER;
    }

    /**
     * Returns true if this is a college sport (higher variance markets).
     */
    public boolean isCollegeSport() {
        return this == CFB || this == CBB || this == WCBB;
    }

    /**
     * ESPN scoreboard path segment for this sport.
     */
    public String getEspnPath() {
        return switch (this) {
            case NFL -> "football/nfl";
            case CFB -> "football/college-football";
            case NBA -> "basketball/nba";
            case WNBA -> "basketball/wnba";
            case CBB -> "basketball/mens-college-basketball";
            case WCBB -> "basketball/womens-college-basketball";
            case MLB -> "baseball/mlb";
            case NHL -> "hockey/nhl";
            case SOCCER -> "soccer/usa.1";
        };
    }
}
