package dev.abhi.scribble;

import dev.abhi.scribble.model.Player;
import dev.abhi.scribble.model.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomGameTest {

    @Test
    void advancesThroughTwoTurnsPerPlayerThenFinishes() {
        Player host = new Player("Host");
        Player guest = new Player("Guest");
        Room room = new Room("ABC123", host);
        room.addPlayer(guest);

        room.begin(List.of(host, guest));

        assertSame(host, room.advanceTurn("apple", 60));
        assertEquals("apple", room.getCurrentWord());
        assertEquals(60, room.getSecondsRemaining());

        assertSame(guest, room.advanceTurn("banana", 60));
        assertEquals(1, room.getRoundNumber());

        assertSame(host, room.advanceTurn("house", 60));
        assertEquals(2, room.getRoundNumber());
        assertSame(guest, room.advanceTurn("river", 60));

        assertNull(room.advanceTurn("unused", 60));
        assertEquals(Room.Status.FINISHED, room.getStatus());
        assertNull(room.getCurrentTurn());
    }

    @Test
    void correctGuessAwardsPointsToGuesserAndDrawer() {
        Player drawer = new Player("Drawer");
        Player guesser = new Player("Guesser");
        Room room = new Room("ABC123", drawer);
        room.addPlayer(guesser);
        room.begin(List.of(drawer, guesser));
        room.advanceTurn("apple", 60);

        room.awardCorrectGuess(guesser);

        assertEquals(10, guesser.getScore());
        assertEquals(5, drawer.getScore());
        assertTrue(guesser.hasGuessedCorrectly());
    }

    @Test
    void timerEndsAtZero() {
        Player host = new Player("Host");
        Player guest = new Player("Guest");
        Room room = new Room("ABC123", host);
        room.addPlayer(guest);
        room.begin(List.of(host, guest));
        room.advanceTurn("apple", 2);

        assertFalse(room.tick());
        assertTrue(room.tick());
    }
}
