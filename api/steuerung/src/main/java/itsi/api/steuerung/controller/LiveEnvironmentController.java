package itsi.api.steuerung.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import itsi.api.steuerung.websocket.LiveEnvironmentWebSocketHandler;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/live-environment")
public class LiveEnvironmentController {
    private final WebClient databaseWebClient;
    private final LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler;

    @Autowired
    public LiveEnvironmentController(WebClient databaseWebClient, LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler) {
        this.databaseWebClient = databaseWebClient;
        this.liveEnvironmentWebSocketHandler = liveEnvironmentWebSocketHandler;
    }

    @PostMapping("/start/{userId}")
    public ResponseEntity<?> startLiveEnvironment(@PathVariable Long userId) {
        // Prüfe, ob Live-Environment existiert
        Map<String, Object> liveEnv = databaseWebClient.get()
                .uri("/api/live-environments/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (liveEnv == null || liveEnv.get("id") == null) {
            // Neues Live-Environment anlegen
            Integer maxVncPort = databaseWebClient.get()
                    .uri("/api/live-environments/max-vnc-port")
                    .retrieve()
                    .bodyToMono(Integer.class)
                    .block();
            int newVncPort = (maxVncPort != null ? maxVncPort : 5900) + 1;
            Map<String, Object> newEnv = new HashMap<>();
            newEnv.put("userId", userId);
            newEnv.put("status", "running");
            newEnv.put("vncHost", "localhost");
            newEnv.put("vncPassword", "password123");
            newEnv.put("vncPort", newVncPort);
            // Backend erstellt ID
            liveEnv = databaseWebClient.post()
                    .uri("/api/live-environments")
                    .bodyValue(newEnv)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } else {
            // Existierendes Live-Environment starten
            liveEnv.put("status", "running");
            liveEnv = databaseWebClient.put()
                    .uri("/api/live-environments/" + liveEnv.get("id"))
                    .bodyValue(liveEnv)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        }
        // WebSocket: noVNC-Port setzen und senden
        int vncPort = (int) liveEnv.get("vncPort");
        int noVncPort = 6000 + (vncPort % 100);
        liveEnv.put("noVncPort", noVncPort);
        liveEnvironmentWebSocketHandler.sendToUser(userId, liveEnv);
        return ResponseEntity.ok(liveEnv);
    }

    @PostMapping("/stop/{userId}")
    public ResponseEntity<?> stopLiveEnvironment(@PathVariable Long userId) {
        Map<String, Object> liveEnv = databaseWebClient.get()
                .uri("/api/live-environments/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (liveEnv == null || liveEnv.get("id") == null) {
            return ResponseEntity.badRequest().body("No live environment found for user " + userId);
        }
        liveEnv.put("status", "stopped");
        liveEnv = databaseWebClient.put()
                .uri("/api/live-environments/" + liveEnv.get("id"))
                .bodyValue(liveEnv)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        // WebSocket: Status senden
        liveEnvironmentWebSocketHandler.sendToUser(userId, liveEnv);
        return ResponseEntity.ok(liveEnv);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createLiveEnvironment(@RequestBody Map<String, Object> newEnv) {
        // Hole den aktuellen max VNC-Port
        Integer maxVncPort = databaseWebClient.get()
                .uri("/api/live-environments/max-vnc-port")
                .retrieve()
                .bodyToMono(Integer.class)
                .block();
        int newVncPort = (maxVncPort != null ? maxVncPort : 5900) + 1;
        newEnv.put("vncPort", newVncPort);
        newEnv.putIfAbsent("vncHost", "localhost");
        // Passwort MUSS gesetzt werden, sonst Fehler
        if (!newEnv.containsKey("vncPassword") || newEnv.get("vncPassword") == null || newEnv.get("vncPassword").toString().isEmpty()) {
            return ResponseEntity.badRequest().body("vncPassword muss angegeben werden!");
        }
        newEnv.putIfAbsent("status", "stopped");
        // Backend erstellt ID
        Map<String, Object> createdEnv = databaseWebClient.post()
                .uri("/api/live-environments")
                .bodyValue(newEnv)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return ResponseEntity.ok(createdEnv);
    }

    @GetMapping("/vnc-port/{userId}")
    public ResponseEntity<?> getVncPortByUserId(@PathVariable Long userId) {
        // Hole das Live-Environment für den User
        Map<String, Object> liveEnv = databaseWebClient.get()
                .uri("/api/live-environments/" + userId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        if (liveEnv == null || liveEnv.get("vncPort") == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Kein VNC-Port für diesen User gefunden!");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("vncPort", liveEnv.get("vncPort"));
        if (liveEnv.get("vncPassword") != null) {
            result.put("vncPassword", liveEnv.get("vncPassword"));
        }
        return ResponseEntity.ok(result);
    }
}
