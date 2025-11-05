package com.group6.SongQueue.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;


public class SongQueueTests {

    private SongQueue queue;

    @BeforeEach
    void setup() {
        queue = new SongQueue();
    }

    @Test
    void testAddSong() {
        Song song = new Song("1", "Song A", "Artist A");
        queue.addSong(song);

        List<Song> songs = queue.getSongsInOrder();
        assertEquals(1, songs.size());
        assertEquals("Song A", songs.get(0).getTitle());
    }

    @Test
    void testUpvoteMovesSongUp() {
        Song s1 = new Song("1", "A", "Artist");
        Song s2 = new Song("2", "B", "Artist");
        queue.addSong(s1);
        queue.addSong(s2);

        // B gets upvoted once, should move above A
        queue.upvoteSong("2");

        List<Song> ordered = queue.getSongsInOrder();
        assertEquals("B", ordered.get(0).getTitle());
        assertEquals("A", ordered.get(1).getTitle());
    }

    @Test
    void testDownvoteMovesSongDown() {
        Song s1 = new Song("1", "A", "Artist");
        Song s2 = new Song("2", "B", "Artist");
        queue.addSong(s1);
        queue.addSong(s2);

        // A gets downvoted, should move below B
        queue.downvoteSong("1");

        List<Song> ordered = queue.getSongsInOrder();
        assertEquals("B", ordered.get(0).getTitle());
        assertEquals("A", ordered.get(1).getTitle());
    }

    @Test
    void testTiesResolvedByTimestamp() throws InterruptedException {
        Song s1 = new Song("1", "First", "Artist");
        Thread.sleep(5); // ensure timestamp difference
        Song s2 = new Song("2", "Second", "Artist");

        queue.addSong(s1);
        queue.addSong(s2);

        // Both have 0 votes, so order = by timestamp (older first)
        List<Song> ordered = queue.getSongsInOrder();
        assertEquals("First", ordered.get(0).getTitle());
        assertEquals("Second", ordered.get(1).getTitle());

        // Now both have 1 vote, should still preserve insertion order
        queue.upvoteSong("1");
        queue.upvoteSong("2");

        ordered = queue.getSongsInOrder();
        assertEquals("First", ordered.get(0).getTitle());
        assertEquals("Second", ordered.get(1).getTitle());
    }

    @Test
    void testMultipleVotesAffectOrder() {
        Song s1 = new Song("1", "A", "Artist");
        Song s2 = new Song("2", "B", "Artist");
        Song s3 = new Song("3", "C", "Artist");

        queue.addSong(s1);
        queue.addSong(s2);
        queue.addSong(s3);

        // Upvote C twice
        queue.upvoteSong("3");
        queue.upvoteSong("3");
        // Upvote B once
        queue.upvoteSong("2");

        List<Song> ordered = queue.getSongsInOrder();
        assertEquals("C", ordered.get(0).getTitle()); // most votes
        assertEquals("B", ordered.get(1).getTitle());
        assertEquals("A", ordered.get(2).getTitle());
    }

    @Test
    void testDownvotingBelowZero() {
        Song s = new Song("1", "Sad Song", "Artist");
        queue.addSong(s);
        queue.downvoteSong("1");
        queue.downvoteSong("1");
        queue.downvoteSong("1");

        List<Song> ordered = queue.getSongsInOrder();
        assertEquals(-3, ordered.get(0).getVotes());
    }

    @Test
    void testRemoveSong() {
        Song s1 = new Song("1", "A", "Artist");
        Song s2 = new Song("2", "B", "Artist");
        Song s3 = new Song("3", "C", "Artist");

        queue.addSong(s1);
        queue.addSong(s2);
        queue.addSong(s3);

        //remove song B
        queue.removeSong("2");

        List<Song> ordered = queue.getSongsInOrder();
        assertEquals("A", ordered.get(0).getTitle());
        assertEquals("C", ordered.get(1).getTitle());
        assertEquals(2, ordered.size());

    }


}