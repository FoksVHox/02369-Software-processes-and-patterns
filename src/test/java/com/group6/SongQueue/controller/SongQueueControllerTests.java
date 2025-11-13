package com.group6.SongQueue.controller;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import com.group6.SongQueue.model.Song;
import com.group6.SongQueue.model.SongQueue;

import jakarta.servlet.http.HttpSession;


public class SongQueueControllerTests {
    private SongQueueController songQueueController;

    private HttpSession session;

    @BeforeEach
    void setup() {
        songQueueController = new SongQueueController();
        session = new MockHttpSession();
        session.setAttribute("spotify_access_token", "token");
    }

    @Test
    void createEmptySongQueue() {
        assertEquals(-1, songQueueController.getSongCount(session));
        songQueueController.createSongQueue(session);
        assertEquals(0, songQueueController.getSongCount(session));
    }

    @Test
    void createFilledSongQueue() {
        assertEquals(-1, songQueueController.getSongCount(session));
        songQueueController.createSongQueue(session, new SongQueue(getTestSongs()));
        assertEquals(9, songQueueController.getSongCount(session));
    }

    @Test
    void deleteSongQueue() {
        assertEquals(-1, songQueueController.getSongCount(session));
        songQueueController.createSongQueue(session);
        assertEquals(0, songQueueController.getSongCount(session));
        songQueueController.deleteSongQueue(session);
        assertEquals(-1, songQueueController.getSongCount(session));
    }

    @Test
    void addSong() {
        createEmptySongQueue();
        assertEquals(0, songQueueController.getSongCount(session));
        for(int i = 0; i < 10; i++) {
            songQueueController.addSong(session, new Song(i + "", "Song title", "Test", null));
            assertEquals(i + 1, songQueueController.getSongCount(session));
        }
    }

    @Test
    void votingChangesOrder() {
        createFilledSongQueue();

        List<Song> songs = songQueueController.getSongsInOrder(session);
        assertEquals(songQueueController.getSongCount(session), songs.size());
        assertEquals("id1", songs.get(0).getId());
        assertEquals("id2", songs.get(1).getId());
        assertEquals("id8", songs.get(songs.size() - 2).getId());
        assertEquals("id9", songs.get(songs.size() - 1).getId());

        songQueueController.upvoteSong(session, "id3");
        songQueueController.upvoteSong(session, "id3");
        songQueueController.upvoteSong(session, "id5");
        songQueueController.downvoteSong(session, "id7");
        songQueueController.downvoteSong(session, "id7");
        songQueueController.downvoteSong(session, "id4");

        songs = songQueueController.getSongsInOrder(session);
        assertEquals(songQueueController.getSongCount(session), songs.size());
        assertEquals("id3", songs.get(0).getId());
        assertEquals("id5", songs.get(1).getId());
        assertEquals("id4", songs.get(songs.size() - 2).getId());
        assertEquals("id7", songs.get(songs.size() - 1).getId());
    }

    @Test
    void concurrentSongQueues() {
        HttpSession sessionA = new MockHttpSession();
        sessionA.setAttribute("spotify_access_token", "tokenA");
        HttpSession sessionB = new MockHttpSession();
        sessionB.setAttribute("spotify_access_token", "tokenB");

        assertEquals(-1, songQueueController.getSongCount(sessionA));
        songQueueController.createSongQueue(sessionA);
        assertEquals(0, songQueueController.getSongCount(sessionA));

        assertEquals(-1, songQueueController.getSongCount(sessionB));
        songQueueController.createSongQueue(sessionB);
        assertEquals(0, songQueueController.getSongCount(sessionB));

        songQueueController.addSong(sessionA, new Song("1", "Song 1", "Test", null));
        songQueueController.addSong(sessionA, new Song("2", "Song 2", "Test", null));
        assertEquals(2, songQueueController.getSongCount(sessionA));
        assertEquals(0, songQueueController.getSongCount(sessionB));

        songQueueController.addSong(sessionB, new Song("1", "Song 1", "Test", null));
        assertEquals(2, songQueueController.getSongCount(sessionA));
        assertEquals(1, songQueueController.getSongCount(sessionB));
    }


    private List<Song> getTestSongs() {
        ArrayList<Song> list = new ArrayList<>();
        for(int i = 0; i < 9; i++) {
            list.add(new Song("id" + (i + 1), "Song " + (i + 1), "Test", null));
            try { Thread.sleep(1); } catch (InterruptedException e) {}
        }
        return list;
    }

}