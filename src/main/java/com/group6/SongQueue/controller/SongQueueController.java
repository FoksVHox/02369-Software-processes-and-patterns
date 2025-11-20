package com.group6.SongQueue.controller;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        private static final int JOIN_CODE_LENGTH = 6;

        private final Map<String, SongQueue> activeSongQueues;
        private final Map<String, String> joinCodesByQueue;
        private final Map<String, String> queueByJoinCode;
        private final SecureRandom random;
        private final Map<String, Set<HttpSession>> participantsByQueue;

        SongQueueController() {
                activeSongQueues = new HashMap<>();
                joinCodesByQueue = new HashMap<>();
                queueByJoinCode = new HashMap<>();
                random = new SecureRandom();
                participantsByQueue = new HashMap<>();
        }

        public void createSongQueue(HttpSession session) {
                createSongQueue(session, new SongQueue());
        }

        public void createSongQueue(HttpSession session, SongQueue songQueue) {
                String accessToken = (String) session.getAttribute("spotify_access_token");
                if (accessToken == null) return;

                participantsByQueue.remove(accessToken);
                activeSongQueues.put(accessToken, songQueue);
                assignJoinCode(accessToken);
        }

        public void deleteSongQueue(HttpSession session) {
                String accessToken = (String) session.getAttribute("spotify_access_token");
                if (accessToken == null) return;

                if (!activeSongQueues.containsKey(accessToken)) return; // A song queue doesn't exists for this access token

                activeSongQueues.remove(accessToken);
                String code = joinCodesByQueue.remove(accessToken);
                if (code != null) queueByJoinCode.remove(code);
                session.removeAttribute("userVotes");
                session.removeAttribute("userVetoes");
                ejectParticipants(accessToken, "The host ended the session.");
        }

        public Integer getSongCount(HttpSession session) {
                String key = resolveQueueKey(session);
                if (key == null) return -1;
                return activeSongQueues.get(key).size();
        }

        public List<Song> getSongsInOrder(HttpSession session) {
                String key = resolveQueueKey(session);
                if (key == null) return List.of();
                return activeSongQueues.get(key).getSongsInOrder();
        }

        public void addSong(HttpSession session, Song song) {
                String key = resolveQueueKey(session);
                if (key == null) return;
                activeSongQueues.get(key).addSong(song);
        }

        public void upvoteSong(HttpSession session, String songId) {
                String key = resolveQueueKey(session);
                SongQueue queue = key == null ? null : activeSongQueues.get(key);
                if (queue != null) {
                        queue.upvoteSong(songId);
                }
        }

        public void downvoteSong(HttpSession session, String songId) {
                String key = resolveQueueKey(session);
                SongQueue queue = key == null ? null : activeSongQueues.get(key);
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

				//System.out.println("Current song: " + currentSong.toString());
				songQueue.setCurrentlyPlayingSong(currentSong);

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
			}
		}
	}

        public Song getCurrentlyPlayingSong(HttpSession session) {
                String key = resolveQueueKey(session);
                if (key == null) return null;
                SongQueue queue = activeSongQueues.get(key);
                if (queue != null) {
                        return queue.getCurrentlyPlayingSong();
                }
                log.warn("No active song queue for key: {}", key);
                return null;
        }

        public boolean joinQueue(HttpSession session, String joinCode) {
                if (joinCode == null) return false;
                String normalized = joinCode.trim().toUpperCase();
                if (normalized.isEmpty()) return false;

                String queueKey = queueByJoinCode.get(normalized);
                if (queueKey == null || !activeSongQueues.containsKey(queueKey)) {
                        return false;
                }

                session.setAttribute("joined_queue_key", queueKey);
                session.removeAttribute("userVotes");
                session.removeAttribute("userVetoes");
                session.removeAttribute("sessionClosedMessage");
                participantsByQueue.computeIfAbsent(queueKey, k -> new HashSet<>()).add(session);
                return true;
        }

        public void leaveQueue(HttpSession session) {
                String joinedKey = (String) session.getAttribute("joined_queue_key");
                session.removeAttribute("joined_queue_key");
                session.removeAttribute("userVotes");
                session.removeAttribute("userVetoes");
                if (joinedKey != null) {
                        participantsByQueue.computeIfPresent(joinedKey, (key, set) -> {
                                set.remove(session);
                                return set.isEmpty() ? null : set;
                        });
                }
        }

        public boolean isInQueue(HttpSession session) {
                return resolveQueueKey(session) != null;
        }

        public boolean isHostSession(HttpSession session) {
                String accessToken = (String) session.getAttribute("spotify_access_token");
                return accessToken != null && activeSongQueues.containsKey(accessToken);
        }

        public String getJoinCode(HttpSession session) {
                String key = resolveQueueKey(session);
                if (key == null) return null;
                return joinCodesByQueue.get(key);
        }

        public String getQueueOwnerToken(HttpSession session) {
                String accessToken = (String) session.getAttribute("spotify_access_token");
                if (accessToken != null && (activeSongQueues.containsKey(accessToken) || !joinCodesByQueue.containsKey(accessToken))) {
                        return accessToken;
                }

                String joinedKey = (String) session.getAttribute("joined_queue_key");
                if (joinedKey != null && activeSongQueues.containsKey(joinedKey)) {
                        return joinedKey;
                }
                return null;
        }

        private String resolveQueueKey(HttpSession session) {
                String accessToken = (String) session.getAttribute("spotify_access_token");
                if (accessToken != null && activeSongQueues.containsKey(accessToken)) {
                        return accessToken;
                }

                String joinedKey = (String) session.getAttribute("joined_queue_key");
                if (joinedKey != null && activeSongQueues.containsKey(joinedKey)) {
                        return joinedKey;
                }

                return null;
        }

        private void assignJoinCode(String queueKey) {
                String existing = joinCodesByQueue.remove(queueKey);
                if (existing != null) {
                        queueByJoinCode.remove(existing);
                }

                String code = generateJoinCode();
                joinCodesByQueue.put(queueKey, code);
                queueByJoinCode.put(code, queueKey);
        }

        private String generateJoinCode() {
                Set<String> existingCodes = new HashSet<>(queueByJoinCode.keySet());
                String code;
                do {
                        code = random.ints('A', 'Z' + 1)
                                .limit(JOIN_CODE_LENGTH)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                                .toString();
                } while (existingCodes.contains(code));
                return code;
        }

        private void ejectParticipants(String queueKey, String message) {
                Set<HttpSession> participants = participantsByQueue.remove(queueKey);
                if (participants == null) {
                        return;
                }

                for (HttpSession participant : participants) {
                        participant.removeAttribute("joined_queue_key");
                        participant.removeAttribute("userVotes");
                        if (message != null && !message.isBlank()) {
                                participant.setAttribute("sessionClosedMessage", message);
                        }
                }
        }

    public SongQueue getQueue(HttpSession session) {
        String key = resolveQueueKey(session);
        if (key == null) return null;
        return activeSongQueues.get(key);
    }
}
