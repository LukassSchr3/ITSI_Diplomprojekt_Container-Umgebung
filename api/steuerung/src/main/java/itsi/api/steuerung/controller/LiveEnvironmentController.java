package itsi.api.steuerung.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import itsi.api.steuerung.websocket.LiveEnvironmentWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/live-environment")
@Slf4j
public class LiveEnvironmentController {
    private final WebClient databaseWebClient;
    private final WebClient backendWebClient;
    private final LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler;

    @Autowired
    public LiveEnvironmentController(WebClient databaseWebClient,
                                    @Qualifier("backendWebClient") WebClient backendWebClient,
                                    LiveEnvironmentWebSocketHandler liveEnvironmentWebSocketHandler) {
        this.databaseWebClient = databaseWebClient;
        this.backendWebClient = backendWebClient;
        this.liveEnvironmentWebSocketHandler = liveEnvironmentWebSocketHandler;
    }

    @PostMapping("/start/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<?> startLiveEnvironment(@PathVariable Long userId) {
        try {
            // Prüfe, ob Live-Environment existiert - neuer Endpunkt nach Vorgabe
            Map<String, Object> liveEnv = databaseWebClient.get()
                    .uri("/api/live-environments/user/" + userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (liveEnv == null || liveEnv.get("id") == null) {
                // Neues Live-Environment anlegen: Port basierend auf userId oder max-VNC-Port
                Map<String, Object> newEnv = new HashMap<>();
                newEnv.put("userId", userId);
                newEnv.put("status", "running");
                newEnv.put("vncHost", "localhost");
                newEnv.put("vncPassword", "password123");

                // Versuche, den aktuellen maximalen VNC-Port aus der DB zu lesen (Tests stubben bodyToMono(Integer.class))
                Integer maxVncPort = null;
                try {
                    maxVncPort = databaseWebClient.get()
                            .uri("/api/live-environments/max-vnc-port")
                            .retrieve()
                            .bodyToMono(Integer.class)
                            .block();
                } catch (Exception e) {
                    log.debug("Could not retrieve max VNC port from database", e);
                }

                int newVncPort;
                if (maxVncPort != null) {
                    newVncPort = maxVncPort + 1;
                } else {
                    newVncPort = 5900 + (userId != null ? userId.intValue() : 0);
                }
                newEnv.put("vncPort", newVncPort);

                // Erstelle in Datenbank über den neuen user-Pfad
                liveEnv = databaseWebClient.post()
                        .uri("/api/live-environments/user/" + userId)
                        .bodyValue(newEnv)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
            }

            // Sende START an Backend - mit name, vncHost, vncPort, vncPassword
            try {
                // Hole User aus DB
                Map<String, Object> user = databaseWebClient.get()
                        .uri("/api/users/" + userId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (user == null || user.get("name") == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
                }

                Number vncPortNum = (Number) liveEnv.get("vncPort");
                int vncPortInt = vncPortNum != null ? vncPortNum.intValue() : (5900 + userId.intValue());

                Map<String, Object> backendRequest = new HashMap<>();
                backendRequest.put("name", user.get("name").toString());
                backendRequest.put("port", String.valueOf(vncPortInt));

                log.info("Sending to Backend /live/start: {}", backendRequest);

                Map<String, Object> backendResponse = backendWebClient.post()
                        .uri("/live/start")
                        .bodyValue(backendRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                log.info("Backend response: {}", backendResponse);
            } catch (Exception backendError) {
                log.error("Backend error in /live/start", backendError);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Backend: " + backendError.getMessage()));
            }

            // Update Status in Datenbank über user-Pfad
            liveEnv.put("status", "running");
            liveEnv = databaseWebClient.put()
                    .uri("/api/live-environments/user/" + userId)
                    .bodyValue(liveEnv)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // WebSocket: noVNC-Port setzen und senden
            Number vncPortNum = (Number) liveEnv.get("vncPort");
            int vncPort = vncPortNum != null ? vncPortNum.intValue() : (5900 + (userId != null ? userId.intValue() : 0));
            int noVncPort = 6000 + (vncPort % 100);
            liveEnv.put("noVncPort", noVncPort);
            liveEnvironmentWebSocketHandler.sendToUser(userId, liveEnv);

            log.info("Live environment started for user {}: {}", userId, liveEnv);
            return ResponseEntity.ok(liveEnv);
        } catch (Exception e) {
            log.error("Failed to start live environment for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/stop/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<?> stopLiveEnvironment(@PathVariable Long userId) {
        try {
            Map<String, Object> liveEnv = databaseWebClient.get()
                    .uri("/api/live-environments/user/" + userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (liveEnv == null || liveEnv.get("id") == null) {
                return ResponseEntity.badRequest().body("No live environment found for user " + userId);
            }

            // Sende STOP an Backend - mit name, vncHost, vncPort, vncPassword
            try {
                // Hole User aus DB
                Map<String, Object> user = databaseWebClient.get()
                        .uri("/api/users/" + userId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (user == null || user.get("name") == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
                }

                Map<String, Object> backendRequest = new HashMap<>();
                backendRequest.put("name", user.get("name").toString());

                log.info("Sending to Backend /live/stop: {}", backendRequest);

                Map<String, Object> backendResponse = backendWebClient.post()
                        .uri("/live/stop")
                        .bodyValue(backendRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                log.info("Backend response: {}", backendResponse);
            } catch (Exception backendError) {
                log.error("Backend error in /live/stop", backendError);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Backend: " + backendError.getMessage()));
            }

            // Update Status in Datenbank über user-Pfad
            liveEnv.put("status", "stopped");
            liveEnv = databaseWebClient.put()
                    .uri("/api/live-environments/user/" + userId)
                    .bodyValue(liveEnv)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // WebSocket: Status senden
            liveEnvironmentWebSocketHandler.sendToUser(userId, liveEnv);

            log.info("Live environment stopped for user {}: {}", userId, liveEnv);
            return ResponseEntity.ok(liveEnv);
        } catch (Exception e) {
            log.error("Failed to stop live environment for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<?> resetLiveEnvironment(@PathVariable Long userId) {
        try {
            Map<String, Object> liveEnv = databaseWebClient.get()
                    .uri("/api/live-environments/user/" + userId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (liveEnv == null || liveEnv.get("id") == null) {
                return ResponseEntity.badRequest().body("No live environment found for user " + userId);
            }

            // Sende RESET an Backend - mit name, vncHost, vncPort, vncPassword
            try {
                // Hole User aus DB
                Map<String, Object> user = databaseWebClient.get()
                        .uri("/api/users/" + userId)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                if (user == null || user.get("name") == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
                }

                Number vncPortNumReset = (Number) liveEnv.get("vncPort");
                int vncPortIntReset = vncPortNumReset != null ? vncPortNumReset.intValue() : (5900 + userId.intValue());

                Map<String, Object> backendRequest = new HashMap<>();
                backendRequest.put("name", user.get("name").toString());
                backendRequest.put("port", String.valueOf(vncPortIntReset));

                log.info("Sending to Backend /live/reset: {}", backendRequest);

                Map<String, Object> backendResponse = backendWebClient.post()
                        .uri("/live/reset")
                        .bodyValue(backendRequest)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();

                log.info("Backend response: {}", backendResponse);
            } catch (Exception backendError) {
                log.error("Backend error in /live/reset", backendError);
                return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                        .body(Map.of("error", "Backend: " + backendError.getMessage()));
            }

            // Update Status in Datenbank über user-Pfad
            liveEnv.put("status", "running");
            liveEnv = databaseWebClient.put()
                    .uri("/api/live-environments/user/" + userId)
                    .bodyValue(liveEnv)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            // WebSocket: Status senden
            Number vncPortNum = (Number) liveEnv.get("vncPort");
            int vncPort = vncPortNum != null ? vncPortNum.intValue() : (5900 + (userId != null ? userId.intValue() : 0));
            int noVncPort = 6000 + (vncPort % 100);
            liveEnv.put("noVncPort", noVncPort);
            liveEnvironmentWebSocketHandler.sendToUser(userId, liveEnv);

            log.info("Live environment reset for user {}: {}", userId, liveEnv);
            return ResponseEntity.ok(liveEnv);
        } catch (Exception e) {
            log.error("Failed to reset live environment for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN')")
    public ResponseEntity<?> createLiveEnvironment(@RequestBody Map<String, Object> newEnv) {
        // userId required
        if (newEnv == null || !newEnv.containsKey("userId")) {
            return ResponseEntity.badRequest().body("userId muss angegeben werden!");
        }
        Object userIdObj = newEnv.get("userId");
        int userId;
        try {
            userId = userIdObj instanceof Number ? ((Number) userIdObj).intValue() : Integer.parseInt(userIdObj.toString());
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body("userId ungültig!");
        }

        // vncPassword required and non-empty
        if (!newEnv.containsKey("vncPassword") || newEnv.get("vncPassword") == null || newEnv.get("vncPassword").toString().isEmpty()) {
            return ResponseEntity.badRequest().body("vncPassword muss angegeben werden!");
        }

        // Set default vncHost if missing
        if (!newEnv.containsKey("vncHost") || newEnv.get("vncHost") == null || newEnv.get("vncHost").toString().isEmpty()) {
            newEnv.put("vncHost", "localhost");
        }

        // Set default vncPort if missing; if string attempt parse
        if (!newEnv.containsKey("vncPort") || newEnv.get("vncPort") == null) {
            newEnv.put("vncPort", 5900 + userId);
        } else {
            Object p = newEnv.get("vncPort");
            if (p instanceof String) {
                try {
                    newEnv.put("vncPort", Integer.parseInt((String) p));
                } catch (NumberFormatException ex) {
                    return ResponseEntity.badRequest().body("vncPort muss eine Zahl sein!");
                }
            }
        }

        newEnv.putIfAbsent("status", "stopped");

        // Backend erstellt ID - POST an den user-spezifischen Pfad
        Map<String, Object> createdEnv = databaseWebClient.post()
                .uri("/api/live-environments/user/" + userId)
                .bodyValue(newEnv)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
        return ResponseEntity.ok(createdEnv);
    }

    @GetMapping("/vnc-port/{userId}")
    @PreAuthorize("hasAnyRole('LEHRER','ADMIN') or @securityService.isOwner(#userId)")
    public ResponseEntity<?> getVncPortByUserId(@PathVariable Long userId) {
        // Hole das Live-Environment für den User über user-Pfad
        Map<String, Object> liveEnv = databaseWebClient.get()
                .uri("/api/live-environments/user/" + userId)
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
