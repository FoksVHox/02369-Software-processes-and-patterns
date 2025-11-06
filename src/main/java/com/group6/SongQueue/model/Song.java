package com.group6.SongQueue.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Represents a song in the queue with voting functionality.
 */
public class Song implements Comparable<Song> {
	private final String id;      // Spotify track URI
    private final String title;
    private final String artist;
    private int votes;
    private final long timestamp; // time added, to break ties

    public Song(String id, String title, String artist) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.votes = 0;
        this.timestamp = System.currentTimeMillis();
    }

    public Song(JsonNode apiItem) {
        this.id = apiItem.path("uri").asText();
        this.title = apiItem.path("name").asText("Unknown");
        this.artist = apiItem.path("artists").elements().next().path("name").asText("Unknown");
        this.votes = 0;
        this.timestamp = System.currentTimeMillis();
    }

    // Voting methods
    public void upvote() { votes++; }
    public void downvote() { votes--; }

    public int getVotes() { return votes; }


    /**
     * Compare songs so that higher votes come first, and for equal votes, earlier timestamps come first.
     */
    @Override
    public int compareTo(Song other) {
        int cmp = Integer.compare(other.getVotes(), this.getVotes());
        if (cmp == 0) return Long.compare(this.timestamp, other.timestamp);
        return cmp;
    }

    public String getId() { return id; }

    public String getTitle() { return title; }

    public String getArtist() { return artist; }

    @Override
    public String toString() {
    	return String.format("Song(%s): %s by %s. Votes=%d", id, title, artist, votes);
    }
}
