package dev.abhi.scribble.model;

import java.util.UUID;

public class Player {
    private final UUID id;
    private final String username;
    private String roomID;
    private int score;
    private boolean guessedCorrectly;

    public Player(String username) {
        this.id = UUID.randomUUID();
        this.username = username;
        this.score = 0;
    }

    public UUID getId() { return id; }
    public String getUsername() { return username; }
    public String getRoomID() { return roomID; }
    public void setRoomID(String roomID) { this.roomID = roomID; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public boolean hasGuessedCorrectly() { return guessedCorrectly; }
    public void setGuessedCorrectly(boolean guessedCorrectly) { this.guessedCorrectly = guessedCorrectly; }
}
