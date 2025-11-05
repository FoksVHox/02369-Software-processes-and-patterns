package com.group6.SongQueue.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.client.RestTemplate;

@Controller
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
    private static final String SPOTIFY_ME_URL = "https://api.spotify.com/v1/me";

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        String accessToken = (String) session.getAttribute("spotify_access_token");

        if (accessToken == null) {
            log.info("No Spotify session found, redirecting to /login");
            return "redirect:/login";
        }

        try {
            RestTemplate rest = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = rest.exchange(
                    SPOTIFY_ME_URL, HttpMethod.GET, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("Failed to fetch Spotify profile. Status: {}", response.getStatusCode());
                model.addAttribute("error", "Unable to fetch Spotify profile. Please re-login.");
                return "dashboard";
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response.getBody());
            String displayName = json.path("display_name").asText("(unknown)");
            String email = json.path("email").asText("(no email)");
            String profileUrl = json.path("external_urls").path("spotify").asText(null);

            model.addAttribute("name", displayName);
            model.addAttribute("email", email);
            model.addAttribute("profileUrl", profileUrl);

            return "dashboard";

        } catch (Exception ex) {
            log.error("Error fetching Spotify user info", ex);
            model.addAttribute("error", "Error fetching Spotify profile: " + ex.getMessage());
            return "dashboard";
        }
    }
}
