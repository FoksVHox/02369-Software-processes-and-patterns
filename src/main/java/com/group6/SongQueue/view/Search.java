package com.group6.SongQueue.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequestMapping("/search")
public class Search {

	private static final Logger log = LoggerFactory.getLogger(DashboardController.class);
	private static final String SPOTIFY_SEARCH_URL = "https://api.spotify.com/v1/search";

	@Autowired
	private SongQueueController songQueueController;

	@GetMapping("")
	public String search(@RequestParam(name = "query", required = false) String searchQuery, HttpSession session, Model model) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return Login.redirectToLogin("/search" + (searchQuery == null ? "" : "?query=" + searchQuery), session);

		if(searchQuery == null || searchQuery == "") {
			model.addAttribute("songs", new ArrayList<>());
			return "search";
		}
		model.addAttribute("query", searchQuery);

		try {
			RestTemplate rest = new RestTemplate();

			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

			HttpEntity<Void> request = new HttpEntity<>(headers);

			String url = UriComponentsBuilder.fromUriString(SPOTIFY_SEARCH_URL)
				.queryParam("q", searchQuery)
				.queryParam("type", "track")
                .queryParam("limit", 50)
                .build()
                .toUriString();

			ResponseEntity<String> response = rest.exchange(url, HttpMethod.GET, request, String.class);
			if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) throw new Exception("HTTP Status: {}");

			ObjectMapper mapper = new ObjectMapper();
			JsonNode json = mapper.readTree(response.getBody()).path("tracks");

			List<Song> songs = new ArrayList<>();

			Iterator<JsonNode> elements = json.path("items").elements();
			while(elements.hasNext()) {
				songs.add(new Song(elements.next()));
			}
			model.addAttribute("songs", songs);

			return "search";

		} catch (Exception ex) {
			log.error("Error searching Spotify", ex);
			model.addAttribute("error", "Error seaching Spotify for songs.");
			model.addAttribute("songs", new ArrayList<>());
			return "search";
		}
	}
}
