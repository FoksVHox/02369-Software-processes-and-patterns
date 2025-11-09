package com.group6.SongQueue.view;

import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;

import jakarta.servlet.http.HttpSession;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Handles the Spotify-style voting page, where users can upvote/downvote songs in their queue.
 * Votes are applied directly to the Song objects without reordering the immutable SongQueue list.
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
        List<Song> songs;
        try {
            songs = songQueueController.getSongsInOrder(session);
        } catch (Exception e) {
            log.warn("No active song queue: {}", e.getMessage());
            model.addAttribute("error", "No playlist loaded yet. Please add one on the dashboard first.");
            model.addAttribute("songqueue", List.of());
            return "vote";
        }

        if (songs == null || songs.isEmpty()) {
            model.addAttribute("error", "No songs in queue. Please add songs to the queue to start voting.");
            model.addAttribute("songqueue", List.of());
        } else {
            model.addAttribute("songqueue", List.copyOf(songs));
        }

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
            if ("up".equals(vote)) {
                songQueueController.upvoteSong(session, songId);
            } else if ("down".equals(vote)) {
                songQueueController.downvoteSong(session, songId);
            }
        } catch (Exception e) {
            log.warn("Failed to cast vote: {}", e.getMessage());
        }

        return "redirect:/vote";
    }
}
