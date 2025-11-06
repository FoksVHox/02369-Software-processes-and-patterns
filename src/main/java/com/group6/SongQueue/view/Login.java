package com.group6.SongQueue.view;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class Login {

    @Value("${CLIENT_ID:#{null}}")
    private String clientId;

    @Value("${CLIENT_SECRET:#{null}}")
    private String clientSecret;

    @Value("${REDIRECT_URL:#{null}}")
    private String redirectUri;

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SCOPE = "user-read-private user-read-email user-read-playback-state user-read-currently-playing user-modify-playback-state";

    private static final Logger log = LoggerFactory.getLogger(Login.class);

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        String state = generateState();

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("response_type", "code");
        params.add("client_id", clientId);
        params.add("scope", SCOPE);
        params.add("redirect_uri", redirectUri);
        params.add("state", state);

        String url = UriComponentsBuilder.fromUriString(AUTH_URL)
                .queryParams(params)
                .build()
                .toUriString();

        return ResponseEntity.status(302)
                .header(HttpHeaders.LOCATION, url)
                .build();
    }

    @GetMapping("/callback/spotify")
    public ResponseEntity<Void> callback(
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "error", required = false) String error,
            HttpSession session
    ) {
        if (error != null) {
            log.warn("Spotify returned an error: {}", error);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        if (code == null) {
            log.warn("Missing authorization code from Spotify callback.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            RestTemplate rest = new RestTemplate();

            // Basic auth header
            String creds = clientId + ":" + clientSecret;
            String basicAuth = "Basic " + Base64.getEncoder()
                    .encodeToString(creds.getBytes(StandardCharsets.UTF_8));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Authorization", basicAuth);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "authorization_code");
            body.add("code", code);
            body.add("redirect_uri", redirectUri);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

            // Get raw JSON
            ResponseEntity<String> spotifyResponse = rest.postForEntity(
                    "https://accounts.spotify.com/api/token", request, String.class);

            if (!spotifyResponse.getStatusCode().is2xxSuccessful() || spotifyResponse.getBody() == null) {
                log.error("Token exchange failed with status {}", spotifyResponse.getStatusCode());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Parse JSON without a DTO
            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(spotifyResponse.getBody());

            String accessToken  = json.path("access_token").asText(null);
            String refreshToken = json.path("refresh_token").asText(null); // may be null
            String tokenType    = json.path("token_type").asText(null);
            String scope        = json.path("scope").asText(null);
            long   expiresIn    = json.path("expires_in").asLong(0);

            if (accessToken == null) {
                log.error("Spotify response missing access_token: {}", spotifyResponse.getBody());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
            }

            // Store in session
            session.setAttribute("spotify_access_token", accessToken);
            if (refreshToken != null) session.setAttribute("spotify_refresh_token", refreshToken);
            if (tokenType != null)    session.setAttribute("spotify_token_type", tokenType);
            if (scope != null)        session.setAttribute("spotify_scope", scope);
            if (expiresIn > 0)        session.setAttribute("spotify_expires_at",
                    Instant.now().plusSeconds(expiresIn));
            session.setAttribute("spotify_logged_in", true);

            // Redirect to /dashboard
            HttpHeaders redirectHeaders = new HttpHeaders();
            redirectHeaders.setLocation(URI.create("/dashboard"));
            return new ResponseEntity<>(redirectHeaders, HttpStatus.FOUND);

        } catch (Exception ex) {
            log.error("Failed to exchange code for token", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String generateState() {
        byte[] buf = new byte[16];
        new SecureRandom().nextBytes(buf);
        // URL-safe, no padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }
}
