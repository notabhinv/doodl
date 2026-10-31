package dev.abhi.scribble.controller;

import dev.abhi.scribble.service.RoomService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
public class ChatController {
    private final RoomService roomService;

    public ChatController(RoomService roomService) {
        this.roomService = roomService;
    }

    @MessageMapping("/chat")
    public void sendMessage(ChatMessage message) {
        if (message == null) return;
        roomService.processChat(message.roomId(), parseId(message.playerId()), message.message());
    }

    @MessageMapping("/draw")
    public void sendDrawing(DrawMessage message) {
        if (message == null) return;
        roomService.publishDrawing(
                message.roomId(),
                parseId(message.playerId()),
                new RoomService.DrawingMessage(message.roomId(), message.playerId(),
                        message.type(), message.x(), message.y())
        );
    }

    private UUID parseId(String id) {
        try {
            return UUID.fromString(id);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    public record ChatMessage(String roomId, String playerId, String message) {}
    public record DrawMessage(String roomId, String playerId, String type, double x, double y) {}
}
