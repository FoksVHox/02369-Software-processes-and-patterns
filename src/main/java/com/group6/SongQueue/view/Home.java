package com.group6.SongQueue.view;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.group6.SongQueue.controller.SongQueueController;

import jakarta.servlet.http.HttpSession;

@Controller
public class Home {

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
        model.addAttribute("isInQueue", songQueueController.isInQueue(session));
        model.addAttribute("isHost", songQueueController.isHostSession(session));
        model.addAttribute("activeJoinCode", songQueueController.getJoinCode(session));
        return "home";
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
        return "redirect:/";
    }

    @PostMapping("/leave")
    public String leaveQueue(HttpSession session, RedirectAttributes redirectAttributes) {
        songQueueController.leaveQueue(session);
        redirectAttributes.addFlashAttribute("homeMessage", "You have left the session.");
        return "redirect:/";
    }

    @PostMapping("/close-session")
    public String closeSession(HttpSession session, RedirectAttributes redirectAttributes) {
        String accessToken = (String) session.getAttribute("spotify_access_token");
        if (accessToken == null) {
            return Login.redirectToLogin("/", session);
        }

        if (!songQueueController.isHostSession(session)) {
            redirectAttributes.addFlashAttribute("homeError", "Create a queue before ending the session.");
            return "redirect:/";
        }

        songQueueController.deleteSongQueue(session);
        songQueueController.leaveQueue(session);
        redirectAttributes.addFlashAttribute("homeMessage", "Session ended and queue cleared.");
        return "redirect:/";
    }

}
