package com.group6.SongQueue.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;
import com.group6.SongQueue.model.SongQueue;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final String SPOTIFY_ME_URL = "https://api.spotify.com/v1/me";

	@Autowired
	private SongQueueController songQueueController;

	@GetMapping("")
	public String dashboard(HttpSession session, Model model) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return Login.redirectToLogin("/dashboard", session);

		try {
			RestTemplate rest = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

			HttpEntity<Void> request = new HttpEntity<>(headers);

			ResponseEntity<String> response = rest.exchange(
					SPOTIFY_ME_URL, HttpMethod.GET, request, String.class);

			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new Exception("HTTP Status: {}");

			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(response.getBody());
			String displayName = json.path("display_name").asText("(unknown)");
			String email = json.path("email").asText("(no email)");
			String profileUrl = json.path("external_urls").path("spotify").asText(null);

			model.addAttribute("name", displayName);
			model.addAttribute("email", email);
			model.addAttribute("profileUrl", profileUrl);

                        model.addAttribute("songqueue_size", songQueueController.getSongCount(session));
                        model.addAttribute("joinCode", songQueueController.getJoinCode(session));
                        model.addAttribute("isHost", songQueueController.isHostSession(session));

			return "dashboard";

		} catch (Exception ex) {
			log.error("Error fetching Spotify user info", ex);
			model.addAttribute("error", "Error fetching Spotify profile.");
			return "dashboard";
		}
	}

	@PostMapping("/add-playlist")
	public String addPlaylist(@RequestParam("playlist-url") String playlistUrl, HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return Login.redirectToLogin("/dashboard", session);

		// Retrieve playlist from Spotify
		List<Song> songs = new ArrayList<>();
		try {
			String playlistId = playlistUrl.substring(0, playlistUrl.indexOf('?')).replaceFirst("https://open.spotify.com/playlist/", "");
			System.out.println(playlistId);

			RestTemplate rest = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
			HttpEntity<Void> request = new HttpEntity<>(headers);

			String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

			// Get total song count
			ResponseEntity<String> totalResponse = rest.exchange(UriComponentsBuilder.fromUriString(url)
				.queryParam("fields", "total")
				.build()
				.toUriString(),
				HttpMethod.GET, request, String.class);
			if (!totalResponse.getStatusCode().is2xxSuccessful() || totalResponse.getBody() == null)
				throw new Exception("HTTP error (" + totalResponse.getStatusCode() + ")" + totalResponse.getBody());

			Integer totalSongs = new ObjectMapper().readTree(totalResponse.getBody()).path("total").asInt(0);

			// Get all songs
			while(songs.size() < totalSongs) {
				String paramUrl = UriComponentsBuilder.fromUriString(url)
					.queryParam("offset", songs.size())
					.queryParam("limit", 50)
					.queryParam("fields", "items(track(artists,name,uri,album(images(url))))")
					.build()
					.toUriString();
				ResponseEntity<String> response = rest.exchange(paramUrl, HttpMethod.GET, request, String.class);
				if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
					throw new Exception("HTTP error (" + response.getStatusCode() + ")" + response.getBody());

				ObjectMapper mapper = new ObjectMapper();
				JsonNode json = mapper.readTree(response.getBody());

				Iterator<JsonNode> elements = json.path("items").elements();
				while(elements.hasNext()) {
					songs.add(new Song(elements.next().path("track")));
				}
			}
			System.out.println(songs.size() + " songs found.");
		} catch (Exception ex) {
			log.error("Error fetching Spotify playlist.", ex);
			redirectAttributes.addFlashAttribute("error", "Failed to find Spotify playlist. Make sure it is public.");
			return "redirect:/dashboard";
		}

		// Initialize songqueue with playlist
		songQueueController.createSongQueue(session, new SongQueue(songs));

		// Return to dashboard
		return "redirect:/dashboard";
	}

	@PostMapping("/create-songqueue")
	public String createSongqueue(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return Login.redirectToLogin("/dashboard", session);

		// Initialize songqueue
		songQueueController.createSongQueue(session, new SongQueue());

		// Return to dashboard
		return "redirect:/dashboard";
	}

        @PostMapping("/close-session")
        public String closeSession(HttpSession session,
                                   RedirectAttributes redirectAttributes,
                                   @RequestParam(value = "redirect", required = false) String redirectTarget) {
                String redirect = (redirectTarget == null || redirectTarget.isBlank()) ? "/dashboard" : redirectTarget;
                if (!redirect.startsWith("/")) {
                        redirect = "/" + redirect;
                }

                String accessToken = (String) session.getAttribute("spotify_access_token");
                if (accessToken == null) return Login.redirectToLogin(redirect, session);

                if (!songQueueController.isHostSession(session)) {
                        redirectAttributes.addFlashAttribute("error", "Create a queue before ending the session.");
                        return "redirect:" + redirect;
                }

                songQueueController.deleteSongQueue(session);
                songQueueController.leaveQueue(session);

                if ("/".equals(redirect)) {
                        redirectAttributes.addFlashAttribute("homeMessage", "Session ended and queue cleared.");
                } else {
                        redirectAttributes.addFlashAttribute("success", "Session ended and queue cleared.");
                }

                return "redirect:" + redirect;
        }
}
