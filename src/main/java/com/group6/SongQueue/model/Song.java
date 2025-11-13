package com.group6.SongQueue.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Represents a song in the queue with voting functionality.
 */
public class Song implements Comparable<Song> {
    private static final Logger log = LoggerFactory.getLogger(Song.class);

    private final String id;      // Spotify track URI
    private final String title;
    private final String artist;

    private int votes;
    private final long timestamp; // time added, to break ties
    private final String albumArtUrl;

    public Song(String id, String title, String artist, String albumArtURL) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.votes = 0;
        this.timestamp = System.currentTimeMillis();
        this.albumArtUrl = albumArtURL;
    }

    public Song(JsonNode apiItem) {
        this.id = apiItem.path("uri").asText();

        this.title = apiItem.path("name").asText("Unknown");

        var artists = apiItem.path("artists").elements();
        StringBuilder artistString = new StringBuilder();
        while(artists.hasNext()) artistString.append(artists.next().path("name").asText("") + ", ");
        this.artist = artistString.subSequence(0, artistString.length() - 2).toString();

        this.votes = 0;
        this.timestamp = System.currentTimeMillis();

        // extract album art URL from album.images[0].url (highest resolution)
        JsonNode images = apiItem.path("album").path("images");
        String art = null;
        if (images.isArray() && !images.isEmpty()) {
            art = images.get(0).path("url").asText(null);
            if (art == null || art.isEmpty()) {
                log.warn("No album art found for track id={} title={}", this.id, this.title);
                art = null;
            }
        }
        this.albumArtUrl = art;
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

    public String getAlbumArtUrl() { return albumArtUrl; }

    @Override
    public String toString() {
    	return String.format("Song(%s): %s by %s. Votes=%d", id, title, artist, votes);
    }
}
