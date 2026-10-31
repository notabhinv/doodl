package dev.abhi.scribble.mapper;

import org.springframework.stereotype.Component;
import dev.abhi.scribble.dto.PlayerDTO;
import dev.abhi.scribble.model.Player;

@Component
public class PlayerDTOMapper {
    public PlayerDTO apply(Player player) {
        return new PlayerDTO(player.getId(), player.getUsername(), player.getScore());
    }
}
