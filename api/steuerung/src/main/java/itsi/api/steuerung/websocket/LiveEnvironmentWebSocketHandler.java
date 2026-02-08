package itsi.api.steuerung.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Component
public class LiveEnvironmentWebSocketHandler extends TextWebSocketHandler {
    private final WebClient webClient;
    // Map: userId -> WebSocketSession
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Autowired
    public LiveEnvironmentWebSocketHandler(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl("http://localhost:5050").build();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // userId aus der URL extrahieren
        String path = session.getUri().getPath();
        String[] parts = path.split("/");
        Long userId = null;
        for (int i = 0; i < parts.length; i++) {
            if ("live-environment".equals(parts[i]) && i + 1 < parts.length) {
                userId = Long.valueOf(parts[i + 1]);
                break;
            }
        }
        if (userId != null) {
            sessions.put(userId, session);
            // Live-Environment aus DB holen und an Client senden
            String envJson = webClient.get()
                    .uri("/api/live-environments/" + userId)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            session.sendMessage(new TextMessage(envJson));
        } else {
            session.close();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // Optional: Handle incoming messages (z.B. Start/Stop/Reset)
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.values().remove(session);
    }

    // Sende Live-Environment-Update an den User über WebSocket
    public void sendToUser(Long userId, Map<String, Object> liveEnv) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(liveEnv)));
            } catch (Exception e) {
                // Fehler beim Senden ignorieren oder loggen
            }
        }
    }
}
