package com.group6.SongQueue.view;

import com.group6.SongQueue.controller.SongQueueController;
import com.group6.SongQueue.model.Song;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

/**
 * Handles the live playlist page showing the full song queue.
 */
@Controller
@RequestMapping("/live")
public class Live {

    @Autowired
    private SongQueueController songQueueController;

    /**
     * Displays the live playlist page showing the full song queue.
     */
    @GetMapping
    public String showLivePlaylist(HttpSession session, Model model) {
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
            return "live";
        }

        List<Song> songs = songQueueController.getSongsInOrder(session);

        if (songs == null || songs.isEmpty()) {
            model.addAttribute("error", "No songs in queue...");
            model.addAttribute("songqueue", List.of());
        } else {
            model.addAttribute("songqueue", List.copyOf(songs));
        }

        model.addAttribute("currentSong", songQueueController.getCurrentlyPlayingSong(session));
        model.addAttribute("joinCode", songQueueController.getJoinCode(session));
        model.addAttribute("isHost", songQueueController.isHostSession(session));
        return "live";
    }

    @GetMapping("/songlist")
    public String getSongListFragment(HttpSession session, Model model) {
        if (!songQueueController.isInQueue(session)) {
            model.addAttribute("songqueue", List.of());
            return "fragments/songlist :: songlist";
        }

        List<Song> songs = songQueueController.getSongsInOrder(session);

        model.addAttribute("songqueue", songs);
        return "fragments/songlist :: songlist(showVoteButtons=false)";
    }

    @GetMapping("/current")
    public String getCurrentPlayingFragment(HttpSession session, Model model) {
        if (!songQueueController.isInQueue(session)) {
            model.addAttribute("currentSong", null);
        } else {
        	model.addAttribute("currentSong", songQueueController.getCurrentlyPlayingSong(session));
        }
        return "fragments/song :: song(song=${currentSong},showAddButton=false)";
    }
}