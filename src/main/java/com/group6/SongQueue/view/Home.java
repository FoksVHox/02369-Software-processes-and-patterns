package com.group6.SongQueue.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group6.SongQueue.controller.SongQueueController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;

@Controller
public class Home {

	private static final Logger log = LoggerFactory.getLogger(Home.class);

    @Autowired
    private SongQueueController songQueueController;

    @GetMapping(path="/")
    public String getHomeView(HttpSession session, Model model) {
        if (!model.containsAttribute("homeError")) {
            Object closedMessage = session.getAttribute("sessionClosedMessage");
            if (closedMessage != null) {
                model.addAttribute("homeError", closedMessage.toString());
                session.removeAttribute("sessionClosedMessage");
            }
        }
        Boolean isInQueue = songQueueController.isInQueue(session);
        model.addAttribute("isInQueue", isInQueue);
        model.addAttribute("isHost", songQueueController.isHostSession(session));
        model.addAttribute("isLoggedIn", session.getAttribute("spotify_access_token") != null);
        model.addAttribute("activeJoinCode", songQueueController.getJoinCode(session));

        if(isInQueue) {
        	addHostProfile(session, model);
       		return "home-joined";
        } else {
        	return "home-start";
        }
    }

	private void addHostProfile(HttpSession session, Model model) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return;

		if (songQueueController.isInQueue(session)) {
			model.addAttribute("songqueue_size", songQueueController.getSongCount(session));
		}

		try {
			RestTemplate rest = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

			HttpEntity<Void> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = rest.exchange(
					"https://api.spotify.com/v1/me", HttpMethod.GET, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new Exception("HTTP Status: {}");

			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(response.getBody());
			String displayName = json.path("display_name").asText("(unknown)");
			String email = json.path("email").asText("(no email)");
			String profileUrl = json.path("external_urls").path("spotify").asText(null);

			model.addAttribute("name", displayName);
			model.addAttribute("email", email);
			model.addAttribute("profileUrl", profileUrl);
		} catch (Exception ex) {
			log.error("Error fetching Spotify user info", ex);
			model.addAttribute("error", "Error fetching Spotify profile.");
		}
	}

    @PostMapping("/join")
    public String joinQueue(@RequestParam("join-code") String joinCode,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (songQueueController.joinQueue(session, joinCode)) {
            redirectAttributes.addFlashAttribute("homeMessage", "Joined session successfully.");
        } else {
            redirectAttributes.addFlashAttribute("homeError", "Unable to find a session with that code.");
        }
        return "redirect:/vote";
    }

    @PostMapping("/leave")
    public String leaveQueue(HttpSession session, RedirectAttributes redirectAttributes) {
        songQueueController.leaveQueue(session);
        redirectAttributes.addFlashAttribute("homeMessage", "You have left the session.");
        return "redirect:/";
    }

}
