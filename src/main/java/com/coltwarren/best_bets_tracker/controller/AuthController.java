package com.coltwarren.best_bets_tracker.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        String name = principal.getAttributes().getOrDefault("name", "").toString();
        String email = principal.getAttributes().getOrDefault("email", "").toString();
        String picture = principal.getAttributes().getOrDefault("picture", "").toString();

        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "name", name,
                "email", email,
                "picture", picture
        ));
    }
}
