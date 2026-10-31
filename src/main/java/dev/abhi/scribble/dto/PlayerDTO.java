package dev.abhi.scribble.dto;

import java.util.UUID;

public record PlayerDTO(UUID id, String username, int score) {}
