package dev.abhi.scribble.service;

import dev.abhi.scribble.dto.RoomDTO;
import dev.abhi.scribble.mapper.RoomDTOMapper;
import dev.abhi.scribble.model.Player;
import dev.abhi.scribble.model.Room;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RoomService {
    private static final int TURN_DURATION_SECONDS = 60;

    private final Map<String, Room> rooms = new HashMap<>();
    private final WordService wordService;
    private final SimpMessagingTemplate messaging;
    private final RoomDTOMapper roomMapper;

    public RoomService(WordService wordService,
                       SimpMessagingTemplate messaging,
                       RoomDTOMapper roomMapper) {
        this.wordService = wordService;
        this.messaging = messaging;
        this.roomMapper = roomMapper;
    }

    public synchronized Player createRoom(String username) {
        Player host = new Player(username);
        String roomId;
        do {
            roomId = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        } while (rooms.containsKey(roomId));
        rooms.put(roomId, new Room(roomId, host));
        return host;
    }

    public synchronized Player joinRoom(String roomId, String username) {
        Room room = findRoom(roomId);
        if (room.getStatus() != Room.Status.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This game has already started");
        }
        if (room.getPlayers().size() >= 8) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This room is full (maximum 8 players)");
        }
        Player player = new Player(username);
        room.addPlayer(player);
        publishState(room);
        return player;
    }

    public synchronized RoomDTO getRoomState(String roomId) {
        return roomMapper.apply(findRoom(roomId));
    }

    public synchronized PrivateWord getWordForPlayer(String roomId, UUID playerId) {
        Room room = findRoom(roomId);
        Player player = findPlayer(room, playerId);
        if (room.getStatus() != Room.Status.PLAYING || room.getCurrentTurn() != player) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The word is only available to the active drawer");
        }
        return new PrivateWord(room.getCurrentWord());
    }

    public synchronized void startGame(String roomId, UUID requesterId) {
        Room room = findRoom(roomId);
        if (!room.getHost().getId().equals(requesterId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room host can start the game");
        }
        if (room.getStatus() != Room.Status.WAITING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The game has already started");
        }
        if (room.getPlayers().size() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least 2 players are required");
        }
        room.begin(room.getPlayers());
        advanceTurn(room);
    }

    public synchronized void processChat(String roomId, UUID playerId, String rawMessage) {
        Room room = findRoom(roomId);
        Player sender = findPlayer(room, playerId);
        if (rawMessage == null || rawMessage.isBlank()) return;
        String message = rawMessage.trim();
        if (message.length() > 200) message = message.substring(0, 200);

        if (room.getStatus() == Room.Status.PLAYING
                && message.equalsIgnoreCase(room.getCurrentWord())) {
            // Never echo the exact answer into the public chat, even if the drawer or
            // a player who already guessed submits it.
            if (room.getCurrentTurn() != sender && !sender.hasGuessedCorrectly()) {
                room.awardCorrectGuess(sender);
                messaging.convertAndSend(chatTopic(roomId),
                        new ChatEvent(sender.getUsername(), "guessed the word!", true));
                boolean everyoneGuessed = room.getPlayers().stream()
                        .filter(player -> player != room.getCurrentTurn())
                        .allMatch(Player::hasGuessedCorrectly);
                if (everyoneGuessed) {
                    advanceTurn(room);
                } else {
                    publishState(room);
                }
            }
            return;
        }

        messaging.convertAndSend(chatTopic(roomId),
                new ChatEvent(sender.getUsername(), message, false));
    }

    public synchronized void publishDrawing(String roomId, UUID playerId, DrawingMessage drawing) {
        Room room = findRoom(roomId);
        Player sender = findPlayer(room, playerId);
        if (room.getStatus() != Room.Status.PLAYING || room.getCurrentTurn() != sender) return;
        if (!"START".equals(drawing.type()) && !"DRAW".equals(drawing.type()) && !"STOP".equals(drawing.type()) && !"CLEAR".equals(drawing.type())) return;
        messaging.convertAndSend("/topic/room/" + roomId + "/draw", drawing);
    }

    @Scheduled(fixedRate = 1000)
    public synchronized void tickGames() {
        for (Room room : rooms.values()) {
            if (room.tick()) {
                advanceTurn(room);
            } else if (room.getStatus() == Room.Status.PLAYING) {
                publishState(room);
            }
        }
    }

    private void advanceTurn(Room room) {
        Player nextDrawer = room.advanceTurn(wordService.getRandomWord(), TURN_DURATION_SECONDS);
        publishState(room);
        if (nextDrawer != null) {
            messaging.convertAndSend(
                    "/topic/room/" + room.getRoomID() + "/player/" + nextDrawer.getId(),
                    new PrivateWord(room.getCurrentWord())
            );
        }
        if (room.getStatus() == Room.Status.FINISHED) {
            messaging.convertAndSend(chatTopic(room.getRoomID()),
                    new ChatEvent("Game", "Game finished! Check the final scores.", false));
        }
    }

    private void publishState(Room room) {
        messaging.convertAndSend("/topic/room/" + room.getRoomID() + "/state", roomMapper.apply(room));
    }

    private Room findRoom(String roomId) {
        Room room = rooms.get(roomId == null ? "" : roomId.toUpperCase());
        if (room == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        return room;
    }

    private Player findPlayer(Room room, UUID playerId) {
        if (playerId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player ID is required");
        return room.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not in this room"));
    }

    private String chatTopic(String roomId) {
        return "/topic/room/" + roomId + "/chat";
    }

    public record PrivateWord(String word) {}
    public record ChatEvent(String username, String message, boolean correctGuess) {}
    public record DrawingMessage(String roomId, String playerId, String type, double x, double y) {}
}
