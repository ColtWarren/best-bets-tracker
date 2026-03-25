package com.coltwarren.best_bets_tracker.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Value("${app.allowed-email:}")
    private String allowedEmail;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User user = super.loadUser(userRequest);
        String email = user.getAttribute("email");

        // If allowed-email is configured, restrict access to that email only
        if (allowedEmail != null && !allowedEmail.isBlank()) {
            if (!allowedEmail.trim().equalsIgnoreCase(email != null ? email.trim() : "")) {
                log.warn("Access denied for email: {}", email);
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied",
                                "Unauthorized email: " + email, null));
            }
        }

        log.info("OAuth2 login successful for: {}", email);
        return user;
    }
}
