# doodl

A real-time multiplayer drawing and guessing game built with Java, Spring Boot, STOMP over WebSocket, and vanilla JavaScript.

## Features

- Create a private room and invite players with a six-character room code
- Join rooms with a username
- Live player list and scoreboard
- Host-controlled game start (2–8 players)
- Two turns per player, with a 60-second timer per turn
- A random word is sent privately to the active drawer
- Shared drawing canvas with live strokes and board clearing
- Room chat and automatic answer checking
- Correct guess: 10 points for the guesser and 5 points for the drawer

## Requirements

- Java 21 or newer
- Maven (or the included Maven Wrapper, if available)

## Run locally

From the `scribble` directory:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

You can also run `ScribbleApplication` from IntelliJ IDEA.

Open:

```text
http://localhost:8080/
```

Use two browser windows (or two devices on the same network) to test multiplayer. Create a room in one window, join with the room code in the other, then start the game as the host.

## API

| Method | Endpoint | Purpose |
|---|---|---|
| POST | `/game/create` | Create a room |
| POST | `/game/{roomId}/join` | Join a waiting room |
| POST | `/game/{roomId}/start` | Start the game (host only) |
| GET | `/game/{roomId}` | Get public room/game state |

## WebSocket

Connect to `/ws` and use the `/app` application prefix.

| Destination | Purpose |
|---|---|
| `/app/chat` | Send a room chat message or guess |
| `/app/draw` | Send drawing events |

Room updates are broadcast under `/topic/room/{roomId}/...`.

## Current limitations

- Rooms and scores are held in memory. Restarting the server clears all rooms.
- This is a portfolio/demo project, not a production-secure game. Player IDs are client-held identifiers, and there is no account system or abuse protection.
- The active drawer's word is sent on a per-player topic. This keeps it out of the public game-state payload, but a production deployment should add authenticated WebSocket sessions and stronger authorization.
- The game is designed for a single running server instance; it does not use Redis or a shared message broker.
