package dev.abhi.scribble.dto;

import java.util.UUID;

public record PlayerSessionDTO(String roomId, UUID playerId, String username) {}
