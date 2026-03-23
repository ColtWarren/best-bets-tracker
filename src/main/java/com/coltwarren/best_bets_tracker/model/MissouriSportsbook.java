package com.coltwarren.best_bets_tracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "missouri_sportsbooks", indexes = {
        @Index(name = "idx_sportsbook_name", columnList = "name", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
public class MissouriSportsbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String signupBonus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public MissouriSportsbook(String name, boolean active, String signupBonus) {
        this.name = name;
        this.active = active;
        this.signupBonus = signupBonus;
    }
}
