package dev.abhi.scribble.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Room {
    public enum Status { WAITING, PLAYING, FINISHED }

    private final String roomID;
    private final List<Player> players = new ArrayList<>();
    private final List<Player> turnOrder = new ArrayList<>();
    private final Player host;
    private Player currentTurn;
    private String currentWord;
    private Status status = Status.WAITING;
    private int secondsRemaining;
    private int turnIndex = -1;
    private int roundNumber;
    private final int totalRounds = 2;

    public Room(String roomID, Player host) {
        this.roomID = roomID;
        this.host = host;
        addPlayer(host);
    }

    public void addPlayer(Player player) {
        player.setRoomID(roomID);
        players.add(player);
    }

    public void begin(List<Player> wordsOrder) {
        turnOrder.clear();
        turnOrder.addAll(wordsOrder);
        status = Status.PLAYING;
        turnIndex = -1;
        roundNumber = 1;
    }

    public Player advanceTurn(String word, int durationSeconds) {
        turnIndex++;
        if (turnIndex >= turnOrder.size() * totalRounds) {
            status = Status.FINISHED;
            currentTurn = null;
            currentWord = null;
            secondsRemaining = 0;
            return null;
        }
        roundNumber = (turnIndex / turnOrder.size()) + 1;
        currentTurn = turnOrder.get(turnIndex % turnOrder.size());
        currentWord = word;
        secondsRemaining = durationSeconds;
        players.forEach(player -> player.setGuessedCorrectly(false));
        return currentTurn;
    }

    public boolean tick() {
        if (status != Status.PLAYING) return false;
        secondsRemaining--;
        return secondsRemaining <= 0;
    }

    public void awardCorrectGuess(Player guesser) {
        if (guesser == null || currentTurn == null || guesser == currentTurn
                || guesser.hasGuessedCorrectly()) return;
        guesser.addScore(10);
        guesser.setGuessedCorrectly(true);
        currentTurn.addScore(5);
    }

    public String getRoomID() { return roomID; }
    public List<Player> getPlayers() { return players; }
    public Player getHost() { return host; }
    public Player getCurrentTurn() { return currentTurn; }
    public String getCurrentWord() { return currentWord; }
    public Status getStatus() { return status; }
    public int getSecondsRemaining() { return secondsRemaining; }
    public int getRoundNumber() { return roundNumber; }
    public int getTotalRounds() { return totalRounds; }
}
