package dev.abhi.scribble.controller;

import dev.abhi.scribble.dto.PlayerSessionDTO;
import dev.abhi.scribble.dto.RoomDTO;
import dev.abhi.scribble.model.Player;
import dev.abhi.scribble.service.RoomService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/game")
public class GameController {
    private final RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public PlayerSessionDTO createRoom(@RequestBody UsernameRequest request) {
        String username = validateUsername(request.username());
        Player player = roomService.createRoom(username);
        return new PlayerSessionDTO(player.getRoomID(), player.getId(), player.getUsername());
    }

    @PostMapping("/{roomId}/join")
    public PlayerSessionDTO joinRoom(@PathVariable("roomId") String roomId, @RequestBody UsernameRequest request) {
        String username = validateUsername(request.username());
        Player player = roomService.joinRoom(roomId, username);
        return new PlayerSessionDTO(player.getRoomID(), player.getId(), player.getUsername());
    }

    @PostMapping("/{roomId}/start")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void startGame(@PathVariable("roomId") String roomId, @RequestBody StartRequest request) {
        roomService.startGame(roomId, request.playerId());
    }

    @GetMapping("/{roomId}")
    public RoomDTO getRoom(@PathVariable("roomId") String roomId) {
        return roomService.getRoomState(roomId);
    }

    @GetMapping("/{roomId}/word")
    public RoomService.PrivateWord getCurrentWord(@PathVariable("roomId") String roomId,
                                                    @RequestParam("playerId") UUID playerId) {
        return roomService.getWordForPlayer(roomId, playerId);
    }

    private String validateUsername(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        String username = value.trim();
        if (username.length() > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be 16 characters or fewer");
        }
        return username;
    }

    public record UsernameRequest(String username) {}
    public record StartRequest(UUID playerId) {}
}
