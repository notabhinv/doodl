const $ = (id) => document.getElementById(id);

let session = null;
let stompClient = null;
let currentState = null;
let currentWord = "";
let isDrawing = false;

const canvas = $("board");
const ctx = canvas.getContext("2d");

async function api(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: { "Content-Type": "application/json", ...(options.headers || {}) }
    });
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try {
            const body = await response.json();
            message = body.detail || body.message || body.error || message;
        } catch (_) {
            // Keep the HTTP status message when the response is not JSON.
        }
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}

function setLobbyError(message) {
    $("lobbyError").textContent = message || "";
}

$("createForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    setLobbyError("");
    const username = $("createUsername").value.trim();
    if (!username) return;
    try {
        const data = await api("/game/create", {
            method: "POST",
            body: JSON.stringify({ username })
        });
        enterRoom(data);
    } catch (error) {
        setLobbyError(error.message);
    }
});

$("joinForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    setLobbyError("");
    const username = $("joinUsername").value.trim();
    const roomId = $("joinRoomId").value.trim().toUpperCase();
    if (!username || !roomId) return;
    try {
        const data = await api(`/game/${encodeURIComponent(roomId)}/join`, {
            method: "POST",
            body: JSON.stringify({ username })
        });
        enterRoom(data);
    } catch (error) {
        setLobbyError(error.message);
    }
});

function enterRoom(data) {
    session = data;
    $("lobby").classList.add("hidden");
    $("game").classList.remove("hidden");
    $("roomHeading").innerHTML = `Room <span>${escapeHtml(data.roomId)}</span>`;
    $("connectionStatus").textContent = "Connecting…";
    connectWebSocket();
    loadRoomState();
}

async function loadRoomState() {
    try {
        const state = await api(`/game/${encodeURIComponent(session.roomId)}`);
        renderState(state);
        if (state.currentDrawerId === session.playerId && state.status === "PLAYING") {
            const privateWord = await api(`/game/${encodeURIComponent(session.roomId)}/word?playerId=${encodeURIComponent(session.playerId)}`);
            currentWord = privateWord.word || "";
            $("word").textContent = currentWord || "—";
            updateTurnMessage();
        }
    } catch (error) {
        appendSystemMessage(error.message);
    }
}

function connectWebSocket() {
    if (!window.StompJs) {
        $("connectionStatus").textContent = "Chat library failed to load";
        appendSystemMessage("Could not load the WebSocket client. Check your internet connection and refresh.");
        return;
    }

    const scheme = window.location.protocol === "https:" ? "wss" : "ws";
    stompClient = new StompJs.Client({
        brokerURL: `${scheme}://${window.location.host}/ws`,
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        onConnect: () => {
            $("connectionStatus").textContent = "Connected";
            document.querySelector(".status-dot").classList.add("online");

            stompClient.subscribe(`/topic/room/${session.roomId}/state`, (frame) => {
                renderState(JSON.parse(frame.body));
            });
            stompClient.subscribe(`/topic/room/${session.roomId}/chat`, (frame) => {
                renderChatEvent(JSON.parse(frame.body));
            });
            stompClient.subscribe(`/topic/room/${session.roomId}/draw`, (frame) => {
                renderDrawing(JSON.parse(frame.body));
            });
            stompClient.subscribe(`/topic/room/${session.roomId}/player/${session.playerId}`, (frame) => {
                const payload = JSON.parse(frame.body);
                currentWord = payload.word || "";
                $("word").textContent = currentWord || "—";
                updateTurnMessage();
            });
            loadRoomState();
        },
        onWebSocketClose: () => {
            $("connectionStatus").textContent = "Reconnecting…";
            document.querySelector(".status-dot").classList.remove("online");
        },
        onStompError: (frame) => {
            console.error("STOMP error:", frame.headers?.message || frame.body);
            $("connectionStatus").textContent = "Connection error";
        },
        onWebSocketError: (error) => console.error("WebSocket error:", error)
    });
    stompClient.activate();
}

$("startGame").addEventListener("click", async () => {
    $("startGame").disabled = true;
    try {
        await api(`/game/${encodeURIComponent(session.roomId)}/start`, {
            method: "POST",
            body: JSON.stringify({ playerId: session.playerId })
        });
    } catch (error) {
        appendSystemMessage(error.message);
        $("startGame").disabled = false;
    }
});

$("copyCode").addEventListener("click", async () => {
    try {
        await navigator.clipboard.writeText(session.roomId);
        $("copyCode").textContent = "Copied!";
        setTimeout(() => $("copyCode").textContent = "Copy code", 1400);
    } catch (_) {
        $("copyCode").textContent = session.roomId;
    }
});

$("chatForm").addEventListener("submit", (event) => {
    event.preventDefault();
    const input = $("chatInput");
    const message = input.value.trim();
    if (!message) return;
    if (!stompClient?.connected) {
        appendSystemMessage("Chat is reconnecting. Try again in a moment.");
        return;
    }
    stompClient.publish({
        destination: "/app/chat",
        body: JSON.stringify({
            roomId: session.roomId,
            playerId: session.playerId,
            message
        })
    });
    input.value = "";
});

function renderState(state) {
    if (!state) return;
    const oldTurnKey = currentState
        ? `${currentState.currentDrawerId || ""}-${currentState.roundNumber}-${currentState.status}`
        : "";
    const newTurnKey = `${state.currentDrawerId || ""}-${state.roundNumber}-${state.status}`;
    if (oldTurnKey && oldTurnKey !== newTurnKey) {
        clearCanvas();
        currentWord = "";
        $("word").textContent = "—";
    }
    currentState = state;

    $("playerCount").textContent = `${state.players.length}/8`;
    $("playersList").replaceChildren();
    const sortedPlayers = [...state.players].sort((a, b) => b.score - a.score);
    sortedPlayers.forEach((player) => {
        const item = document.createElement("li");
        const name = document.createElement("span");
        name.className = "player-name" + (player.id === state.hostId ? " host" : "");
        name.textContent = player.username + (player.id === session.playerId ? " (you)" : "");
        const score = document.createElement("span");
        score.className = "player-score";
        score.textContent = `${player.score} pts`;
        item.append(name, score);
        $("playersList").appendChild(item);
    });

    $("timer").textContent = state.status === "PLAYING" ? state.secondsRemaining : "—";
    $("roundLabel").textContent = state.status === "WAITING"
        ? "Waiting for the host"
        : state.status === "FINISHED"
            ? "Game over · Final scores"
            : `Round ${state.roundNumber} of ${state.totalRounds}`;

    const isHost = state.hostId === session.playerId;
    const isDrawer = state.currentDrawerId === session.playerId;
    $("startGame").classList.toggle("hidden", !(isHost && state.status === "WAITING"));
    $("startGame").disabled = state.players.length < 2;
    $("startHint").classList.toggle("hidden", !(isHost && state.status === "WAITING"));
    $("startHint").textContent = state.players.length < 2
        ? "Invite at least one more player to start."
        : "You're the host. Start when everyone is ready.";

    $("wordCard").classList.toggle("hidden", state.status !== "PLAYING");
    $("canvasStatus").textContent = state.status === "WAITING"
        ? "Waiting for the game to start"
        : state.status === "FINISHED"
            ? "Game finished"
            : isDrawer ? "Your turn — draw the word!" : `${state.currentDrawer || "Someone"} is drawing`;
    $("turnMessage").textContent = state.status === "WAITING"
        ? "Share your room code. The host can start when 2–8 players have joined."
        : state.status === "FINISHED"
            ? "That's a wrap! Refresh to create a new room."
            : isDrawer
                ? "You're drawing. Keep your word secret!"
                : `${state.currentDrawer} is drawing. Type your guesses in the chat.`;

    $("clearCanvas").disabled = !isDrawer || state.status !== "PLAYING";
    canvas.style.cursor = isDrawer ? "crosshair" : "not-allowed";
    if (state.status !== "PLAYING") {
        currentWord = "";
        $("word").textContent = "—";
    } else if (!isDrawer) {
        $("word").textContent = "Hidden — make a guess";
    } else if (currentWord) {
        $("word").textContent = currentWord;
    }
}

function updateTurnMessage() {
    if (currentState?.currentDrawerId === session.playerId && currentWord) {
        $("turnMessage").textContent = "You're drawing. Keep your word secret!";
    }
}

function renderChatEvent(event) {
    const line = document.createElement("p");
    if (event.correctGuess) {
        line.className = "correct";
        line.textContent = `✳ ${event.username} ${event.message}`;
    } else {
        const user = document.createElement("span");
        user.className = "chat-user";
        user.textContent = `${event.username}: `;
        line.append(user, document.createTextNode(event.message));
    }
    $("chatBox").appendChild(line);
    $("chatBox").scrollTop = $("chatBox").scrollHeight;
}

function appendSystemMessage(message) {
    const line = document.createElement("p");
    line.className = "system-message";
    line.textContent = message;
    $("chatBox").appendChild(line);
    $("chatBox").scrollTop = $("chatBox").scrollHeight;
}

function clearCanvas() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);
}

function pointerPosition(event) {
    const rect = canvas.getBoundingClientRect();
    return {
        x: (event.clientX - rect.left) * canvas.width / rect.width,
        y: (event.clientY - rect.top) * canvas.height / rect.height
    };
}

function publishDrawing(type, point = { x: 0, y: 0 }) {
    if (!stompClient?.connected || !session) return;
    stompClient.publish({
        destination: "/app/draw",
        body: JSON.stringify({
            roomId: session.roomId,
            playerId: session.playerId,
            type,
            x: point.x,
            y: point.y
        })
    });
}

canvas.addEventListener("pointerdown", (event) => {
    if (currentState?.currentDrawerId !== session?.playerId || currentState.status !== "PLAYING") return;
    event.preventDefault();
    canvas.setPointerCapture(event.pointerId);
    isDrawing = true;
    const point = pointerPosition(event);
    ctx.beginPath();
    ctx.moveTo(point.x, point.y);
    ctx.lineWidth = 5;
    ctx.lineCap = "round";
    ctx.lineJoin = "round";
    ctx.strokeStyle = "#24231f";
    publishDrawing("START", point);
});

canvas.addEventListener("pointermove", (event) => {
    if (!isDrawing) return;
    const point = pointerPosition(event);
    ctx.lineTo(point.x, point.y);
    ctx.stroke();
    publishDrawing("DRAW", point);
});

function stopDrawing() {
    if (!isDrawing) return;
    isDrawing = false;
    publishDrawing("STOP");
}
canvas.addEventListener("pointerup", stopDrawing);
canvas.addEventListener("pointercancel", stopDrawing);
canvas.addEventListener("lostpointercapture", stopDrawing);

$("clearCanvas").addEventListener("click", () => {
    if (currentState?.currentDrawerId !== session?.playerId) return;
    clearCanvas();
    publishDrawing("CLEAR");
});

function renderDrawing(drawing) {
    if (drawing.type === "CLEAR") {
        clearCanvas();
    } else if (drawing.type === "START") {
        ctx.beginPath();
        ctx.moveTo(drawing.x, drawing.y);
    } else if (drawing.type === "DRAW") {
        ctx.lineTo(drawing.x, drawing.y);
        ctx.lineWidth = 5;
        ctx.lineCap = "round";
        ctx.lineJoin = "round";
        ctx.strokeStyle = "#24231f";
        ctx.stroke();
    } else if (drawing.type === "STOP") {
        ctx.closePath();
    }
}

function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (char) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[char]);
}

window.addEventListener("beforeunload", () => {
    if (stompClient?.active) stompClient.deactivate();
});
