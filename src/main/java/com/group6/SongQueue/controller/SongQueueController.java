package com.group6.SongQueue.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.group6.SongQueue.model.Song;
import com.group6.SongQueue.model.SongQueue;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.*;

@Controller
public class SongQueueController {

	private static final Logger log = LoggerFactory.getLogger(SongQueueController.class);
	private Map<String, SongQueue> activeSongQueues;

	SongQueueController() {
		activeSongQueues = new HashMap<String, SongQueue>();
	}

	public void createSongQueue(HttpSession session) {
		createSongQueue(session, new SongQueue());
	}

	public void createSongQueue(HttpSession session, SongQueue songQueue) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if(activeSongQueues.containsKey(accessToken)) return; // A song queue already exists for this access token

		activeSongQueues.put(accessToken, songQueue);
	}

	public void deleteSongQueue(HttpSession session) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if(!activeSongQueues.containsKey(accessToken)) return; // A song queue doesn't exists for this access token

		activeSongQueues.remove(accessToken);
	}

	public Integer getSongCount(HttpSession session) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if(!activeSongQueues.containsKey(accessToken)) return -1;
		return activeSongQueues.get(accessToken).size();
	}

	public List<Song> getSongsInOrder(HttpSession session) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		return activeSongQueues.get(accessToken).getSongsInOrder();
	}

	public void addSong(HttpSession session, Song song) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		activeSongQueues.get(accessToken).addSong(song);
	}

	public void upvoteSong(HttpSession session, String songId) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		SongQueue queue = activeSongQueues.get(accessToken);
		if (queue != null) {
			queue.upvoteSong(songId);
		}
	}

	public void downvoteSong(HttpSession session, String songId) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		SongQueue queue = activeSongQueues.get(accessToken);
		if (queue != null) {
			queue.downvoteSong(songId);
		}
	}

	private static final int REFRESH_DELAY = 5000;
	@Scheduled(fixedDelay = REFRESH_DELAY)
	private void updateSpotifyPlayback() {
		for(var current : activeSongQueues.entrySet()) {
			String accessToken = current.getKey();
			SongQueue songQueue = current.getValue();

			if(songQueue.size() == 0) continue; // No songs in queue = nothing to do

			// Shared API stuff
			RestTemplate rest = new RestTemplate();
			HttpHeaders headers = new HttpHeaders();
			headers.setBearerAuth(accessToken);
			headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
			HttpEntity<Void> request = new HttpEntity<>(headers);

			// Get current playback state
			try {
				ResponseEntity<String> response = rest.exchange(
						"https://api.spotify.com/v1/me/player", HttpMethod.GET, request, String.class);

				if (response.getStatusCode().value() == 204) return; // Not playing, nothing to control

				if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
					throw new Exception("HTTP error (" + response.getStatusCode() + ")" + response.getBody());
				}

				ObjectMapper mapper = new ObjectMapper();
				JsonNode json = mapper.readTree(response.getBody());
				Boolean isPlaying = json.path("is_playing").asBoolean();
				Integer progress = json.path("progress_ms").asInt();
				Song currentSong = new Song(json.path("item"));
				Integer songDuration = json.path("item").path("duration_ms").asInt(0);

				System.out.println("Current song: " + currentSong.toString());

				if(!isPlaying) continue; // The Spotify playback is currently paused, we don't want to take control
				if(songDuration - progress >= REFRESH_DELAY) continue; // We are still not at end of current song, do nothing
			} catch (Exception err) {
				log.warn("Failed to fetch Spotify playback state. Error: {}", err.getMessage());
				continue;
			}

			// Set next song
			try {
				Song nextSong = songQueue.nextSong();
				System.out.println("Next song: " + nextSong);

        		String url = UriComponentsBuilder.fromUriString("https://api.spotify.com/v1/me/player/queue")
          			.queryParam("uri", nextSong.getId())
	                .build()
	                .toUriString();

				HttpEntity<Void> nextRequest = new HttpEntity<>(headers);

				ResponseEntity<String> response = rest.exchange(
						url, HttpMethod.POST, nextRequest, String.class);

				if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
					throw new Exception("HTTP error (" + response.getStatusCode() + ")" + response.getBody());
				}
			} catch (Exception err) {
				log.warn("Failed to add song to queue Spotify playback state. Error: {}", err.getMessage());
				continue;
			}
		}
	}

	public Song getCurrentlyPlayingSong(HttpSession session) {
		String accessToken = (String) session.getAttribute("spotify_access_token");
		if (accessToken == null) return null;
		SongQueue queue = activeSongQueues.get(accessToken);
		if (queue != null) {
			return queue.getCurrentlyPlayingSong();
		}
		log.warn("No active song queue for access token: {}", accessToken);
		return null;
	}
}