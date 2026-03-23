package com.coltwarren.best_bets_tracker.controller;

import com.coltwarren.best_bets_tracker.model.MissouriSportsbook;
import com.coltwarren.best_bets_tracker.repository.MissouriSportsbookRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for the Missouri sportsbook reference table.
 *
 * Missouri legalized sports betting in late 2023. This controller
 * provides CRUD access to the list of licensed sportsbooks,
 * which is used to track which book had the best line for each pick.
 */
@RestController
@RequestMapping("/api/sportsbooks")
public class SportsbookController {

    private final MissouriSportsbookRepository sportsbookRepository;

    public SportsbookController(MissouriSportsbookRepository sportsbookRepository) {
        this.sportsbookRepository = sportsbookRepository;
    }

    /**
     * GET /api/sportsbooks
     *
     * Returns all Missouri sportsbooks (active and inactive).
     */
    @GetMapping
    public ResponseEntity<List<MissouriSportsbook>> getAll() {
        return ResponseEntity.ok(sportsbookRepository.findAll());
    }

    /**
     * GET /api/sportsbooks/active
     *
     * Returns only currently active Missouri sportsbooks.
     */
    @GetMapping("/active")
    public ResponseEntity<List<MissouriSportsbook>> getActive() {
        return ResponseEntity.ok(sportsbookRepository.findByActiveTrue());
    }

    /**
     * GET /api/sportsbooks/{id}
     *
     * Returns a single sportsbook by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MissouriSportsbook> getById(@PathVariable Long id) {
        return sportsbookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/sportsbooks
     *
     * Adds a new Missouri sportsbook to the reference table.
     * Rejects duplicates by name.
     *
     * @param sportsbook the sportsbook to add
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody MissouriSportsbook sportsbook) {
        if (sportsbookRepository.existsByName(sportsbook.getName())) {
            return ResponseEntity.badRequest()
                    .body("Sportsbook already exists: " + sportsbook.getName());
        }
        return ResponseEntity.status(201).body(sportsbookRepository.save(sportsbook));
    }

    /**
     * PUT /api/sportsbooks/{id}
     *
     * Updates an existing sportsbook (e.g., toggle active status,
     * update signup bonus info).
     *
     * @param id   the sportsbook ID to update
     * @param updated the updated sportsbook data
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody MissouriSportsbook updated) {
        return sportsbookRepository.findById(id)
                .map(existing -> {
                    existing.setName(updated.getName());
                    existing.setActive(updated.isActive());
                    existing.setSignupBonus(updated.getSignupBonus());
                    existing.setNotes(updated.getNotes());
                    return ResponseEntity.ok(sportsbookRepository.save(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
