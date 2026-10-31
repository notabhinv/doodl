package dev.abhi.scribble.mapper;

import org.springframework.stereotype.Component;
import dev.abhi.scribble.dto.RoomDTO;
import dev.abhi.scribble.model.Player;
import dev.abhi.scribble.model.Room;

@Component
public class RoomDTOMapper {
    private final PlayerDTOMapper playerDTOMapper;

    public RoomDTOMapper(PlayerDTOMapper playerDTOMapper) {
        this.playerDTOMapper = playerDTOMapper;
    }

    public RoomDTO apply(Room room) {
        Player drawer = room.getCurrentTurn();
        return new RoomDTO(
                room.getRoomID(),
                room.getPlayers().stream().map(playerDTOMapper::apply).toList(),
                room.getHost().getId(),
                drawer == null ? null : drawer.getId(),
                drawer == null ? null : drawer.getUsername(),
                room.getStatus().name(),
                room.getSecondsRemaining(),
                room.getRoundNumber(),
                room.getTotalRounds()
        );
    }
}
