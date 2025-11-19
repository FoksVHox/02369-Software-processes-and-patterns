package com.group6.SongQueue.view;

import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles the voting page, where users can upvote/downvote songs in their queue.
 */
@Controller
@RequestMapping("/vote")
public class Vote {

    private static final Logger log = LoggerFactory.getLogger(Vote.class);

    @Autowired
    private SongQueueController songQueueController;

    /**
     * Displays the voting page showing the user's song queue.
     */
    @GetMapping
    public String showVotePage(HttpSession session, Model model) {
        boolean sessionClosed = false;
        Object closedMessage = session.getAttribute("sessionClosedMessage");
        if (closedMessage != null) {
            model.addAttribute("error", closedMessage.toString());
            session.removeAttribute("sessionClosedMessage");
            sessionClosed = true;
        }

        if (!songQueueController.isInQueue(session)) {
            if (!sessionClosed) {
                model.addAttribute("error", "Join a session to start voting on songs.");
            }
            model.addAttribute("songqueue", List.of());
            model.addAttribute("userVotes", Map.of());
            return "vote";
        }

        List<Song> songs = songQueueController.getSongsInOrder(session);

        if (songs == null || songs.isEmpty()) {
            model.addAttribute("error", "No songs in queue. Please add songs to the queue to start voting.");
            model.addAttribute("songqueue", List.of());
        } else {
            model.addAttribute("songqueue", List.copyOf(songs));
        }

        model.addAttribute("userVotes", getOrCreateVoteMap(session));
        model.addAttribute("currentSong", songQueueController.getCurrentlyPlayingSong(session));
        model.addAttribute("joinCode", songQueueController.getJoinCode(session));
        model.addAttribute("isHost", songQueueController.isHostSession(session));
        return "vote";
    }

    /**
     * Handles upvote/downvote requests.
     */
    @PostMapping
    public String castVote(@RequestParam String songId,
                           @RequestParam String vote,
                           HttpSession session) {
        try {
            Map<String, Integer> votes = getOrCreateVoteMap(session);
            int prev = votes.getOrDefault(songId, 0);

            if ("up".equals(vote)) {
                if (prev == 1) {
                    // already upvoted => no-op
                } else if (prev == 0) {
                    // neutral -> up
                    songQueueController.upvoteSong(session, songId);
                    votes.put(songId, 1);
                } else if (prev == -1) {
                    // was down -> remove down and apply up => net +2
                    songQueueController.upvoteSong(session, songId);
                    songQueueController.upvoteSong(session, songId);
                    votes.put(songId, 1);
                }
            } else if ("down".equals(vote)) {
                if (prev == -1) {
                    // already downvoted => no-op
                } else if (prev == 0) {
                    // neutral -> down
                    songQueueController.downvoteSong(session, songId);
                    votes.put(songId, -1);
                } else if (prev == 1) {
                    // was up -> remove up and apply down => net -2
                    songQueueController.downvoteSong(session, songId);
                    songQueueController.downvoteSong(session, songId);
                    votes.put(songId, -1);
                }
            }

            session.setAttribute("userVotes", votes);
        } catch (Exception e) {
            log.warn("Failed to cast vote: {}", e.getMessage());
        }

        return "redirect:/vote";
    }

    @GetMapping("/songlist")
    public String getSongListFragment(HttpSession session, Model model) {
        if (!songQueueController.isInQueue(session)) {
            model.addAttribute("songqueue", List.of());
            model.addAttribute("userVotes", Map.of());
            return "fragments/songlist :: songlist";
        }

        List<Song> songs = songQueueController.getSongsInOrder(session);

        model.addAttribute("songqueue", songs);
        model.addAttribute("userVotes", getOrCreateVoteMap(session));
        model.addAttribute("currentSong", songQueueController.getCurrentlyPlayingSong(session));
        return "fragments/songlist :: songlist(showVoteButtons=true)";
    }

    @GetMapping("/current")
    public String getCurrentPlayingFragment(HttpSession session, Model model) {
        if (!songQueueController.isInQueue(session)) {
            model.addAttribute("currentSong", null);
            return "fragments/currentlyPlaying :: currentlyPlaying";
        }
        model.addAttribute("currentSong", songQueueController.getCurrentlyPlayingSong(session));
        return "fragments/currentlyPlaying :: currentlyPlaying";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> getOrCreateVoteMap(HttpSession session) {
        Object o = session.getAttribute("userVotes");
        if (o instanceof Map) {
            return (Map<String, Integer>) o;
        } else {
            Map<String, Integer> m = new HashMap<>();
            session.setAttribute("userVotes", m);
            return m;
        }
    }
}