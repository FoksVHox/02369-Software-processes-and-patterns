package com.group6.SongQueue.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Manages a queue of songs with voting functionality. Songs are ordered by votes and time added.
 * Uses a list to hold songs and a map for quick access by ID. Uses bubble up and bubble down to sort songs.
 * @see Song
 */
public class SongQueue {
	public SongQueue() {
		songs = new ArrayList<>();
		songMap = new HashMap<>();
	}
	public SongQueue(List<Song> initialSongs) {
		songMap = initialSongs.stream().collect(Collectors.toMap(Song::getId, Function.identity(), (song1, song2) -> { return song1; }));
        songs = new ArrayList<>(songMap.values());
        songs.sort(null);
	}

    /**
     * List of songs in the queue
     */
    private final List<Song> songs;
    /**
     * Map of song IDs to Song objects for quick access
     */
    private final Map<String, Song> songMap;

    /**
     * The currently playing song (not in the queue)
     */
    private Song currentlyPlayingSong = null;
    /**
     * [UNUSED]
     * The amount of vetoes each player starts with.
     */
    private int vetoesPerPlayer = 0; // Not used at the moment since we don't really keep track of guests
    /**
     * The amount of vetoes a song needs to be removed from the queue.
     */
    private int vetoThreshold = Integer.MAX_VALUE;

    /**
     * Adds a new song at the correct position in the queue if it doesn't already exist.
     * @param song The song to add
     */
    public void addSong(Song song) {
        if (!songMap.containsKey(song.getId())) {
            songs.add(song);
            songMap.put(song.getId(), song);
            bubbleUp(songs.size() - 1); // new songs go to their correct spot
        }
    }

    /**
     * Upvotes a song and repositions it in the queue.
     * @param id The ID of the song to upvote
     */
    public void upvoteSong(String id) {
        Song s = songMap.get(id);
        if (s == null) return;
        s.upvote();
        int idx = songs.indexOf(s);
        bubbleUp(idx);
    }

    /**
     * Downvotes a song and repositions it in the queue.
     * @param id The ID of the song to downvote
     */
    public void downvoteSong(String id) {
        Song s = songMap.get(id);
        if (s == null) return;
        s.downvote();
        int idx = songs.indexOf(s);
        bubbleDown(idx);
    }

    /**
     * "Bubble up" = moves song forwards in queue while it has more votes than song above
     * @param songPositionInQueue The songs queue index
     */
    private void bubbleUp(int songPositionInQueue) {
        while (songPositionInQueue > 0 && songs.get(songPositionInQueue).compareTo(songs.get(songPositionInQueue - 1)) < 0) {
            Collections.swap(songs, songPositionInQueue, songPositionInQueue - 1);
            songPositionInQueue--;
        }
    }

    /**
     * "Bubble down" = moves song backwards in queue while it has fewer votes than song below
     * @param songPositionInQueue The songs queue index
     */
    private void bubbleDown(int songPositionInQueue) {
        while (songPositionInQueue < songs.size() - 1 && songs.get(songPositionInQueue).compareTo(songs.get(songPositionInQueue + 1)) > 0) {
            Collections.swap(songs, songPositionInQueue, songPositionInQueue + 1);
            songPositionInQueue++;
        }
    }

    /**
     * Removes a song from the queue.
     * @param id The ID of the song to remove
     */
    public void removeSong(String id) {
        Song s = songMap.get(id);
        if (s == null) return;
        songs.remove(s);
        songMap.remove(id);
    }

    /**
     * Gets the list of songs in the queue in order.
     * @return Unmodifiable list of songs
     */
    public List<Song> getSongsInOrder() {
        return Collections.unmodifiableList(songs);
    }

    /**
     * Gets number of songs in queue.
     * @return Size of queue
     */
    public Integer size() {
        return songs.size();
    }

    /**
     * Gets and removes the next song in the queue.
     * @return Next song
     */
    public Song nextSong() {
        currentlyPlayingSong = songs.removeFirst();
        return currentlyPlayingSong;
    }

    public boolean vetoSong(String id) {
        Song song = songMap.get(id);
        if (song == null) return false;

        song.addVeto();
        if (song.getVetoCount() >= vetoThreshold) {
            removeSong(id);
            return true; // song removed
        }
        return false; // not removed yet
    }


    public Song getCurrentlyPlayingSong() {
        return currentlyPlayingSong;
    }

    public void setCurrentlyPlayingSong(Song song) {
        currentlyPlayingSong = song;
    }

    public void setVetoSettings(int perPlayer, int threshold) {
        this.vetoesPerPlayer = perPlayer;
        this.vetoThreshold = threshold;
    }

    public int getVetoThreshold() { return vetoThreshold; }

    public int getVetoesPerPlayer() { return vetoesPerPlayer; }
}
