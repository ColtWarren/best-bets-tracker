package com.coltwarren.best_bets_tracker.dto;

import lombok.Data;

@Data
public class MlbAssessmentResponseDTO {
    private String homeTeam;
    private String awayTeam;
    private String venue;
    private Double moneylineHome;
    private Double moneylineAway;
    private Double total;
    private String verdict;
    private Integer verdictConfidence;
    private String pitchingEdge;
    private String parkFactor;
    private String reasoning;
    private String bettingType;
    private String assessmentType;
}
