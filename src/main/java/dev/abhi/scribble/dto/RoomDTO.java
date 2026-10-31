package dev.abhi.scribble.dto;

import java.util.List;
import java.util.UUID;

public record RoomDTO(
        String roomId,
        List<PlayerDTO> players,
        UUID hostId,
        UUID currentDrawerId,
        String currentDrawer,
        String status,
        int secondsRemaining,
        int roundNumber,
        int totalRounds
) {}
