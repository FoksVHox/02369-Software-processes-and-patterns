package com.group6.SongQueue.controller;

import org.springframework.stereotype.Controller;

@Controller
public class JoinSessionController {
    private int sessionCode;

    public void startJoinSession() {
        /* TODO: Gør så man kan joine */
        sessionCode = (int)System.currentTimeMillis();
    }

    public void stopJoinSession() {
        /* TODO: Luk og smid alle ud*/
        sessionCode = -1;
    }

    public static void joinSession() {
        /* TODO: Med koden tilføj til sessionen */
    }

    public static void leaveSession() {
        /* TODO: Med en knap forlad sessionen */
    }

    public int getSessionCode() {
        return  sessionCode;
    }
}
