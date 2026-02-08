package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.service.DatabaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/backend")
@Tag(name = "Backend Operations", description = "Direct operations to Go Backend on port 3030")
@Slf4j
public class BackendController {

    private final WebClient backendWebClient;
    private final DatabaseService databaseService;

    public BackendController(@Qualifier("backendWebClient") WebClient backendWebClient,
                           DatabaseService databaseService) {
        this.backendWebClient = backendWebClient;
        this.databaseService = databaseService;
    }

    // ==================== LIVE ENVIRONMENT ====================

    @PostMapping("/live/start")
    @Operation(summary = "Start Live Environment",
               description = "Frontend sends: userId. API fetches data from DB and sends to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> startLiveEnvironment(@RequestParam Integer userId) {
        log.info("Starting live environment for userId: {}", userId);

        try {
            // Hole Live-Environment aus Datenbank
            var liveEnvData = databaseService.getLiveEnvironmentByUserId(userId);

            // Hole User-Daten aus DB
            var userData = databaseService.getUserById(userId);

            // Baue vollständiges Request für Go-Backend
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("userId", userId);

            if (liveEnvData != null) {
                backendRequest.put("liveEnvId", liveEnvData.get("id"));
                backendRequest.put("dockerContainerId", liveEnvData.get("dockerContainerId"));
                backendRequest.put("dockerImage", liveEnvData.get("dockerImage"));
                backendRequest.put("vncHost", liveEnvData.get("vncHost"));
                backendRequest.put("vncPort", liveEnvData.get("vncPort"));
                backendRequest.put("vncPassword", liveEnvData.get("vncPassword"));
                backendRequest.put("status", liveEnvData.get("status"));
            }

            if (userData != null) {
                backendRequest.put("userName", userData.getName());
                backendRequest.put("userEmail", userData.getEmail());
            }

            log.info("Sending to Go backend: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/live/start")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful start (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        if (liveEnvData != null && liveEnvData.get("id") != null) {
                            return databaseService.updateLiveEnvironmentStatus(
                                (Integer) liveEnvData.get("id"),
                                "running"
                            ).thenReturn(ResponseEntity.ok(result));
                        }

                        return Mono.just(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error starting live environment", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });
        } catch (Exception e) {
            log.error("Error preparing live environment start", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    @PostMapping("/live/stop")
    @Operation(summary = "Stop Live Environment",
               description = "Frontend sends: userId. API fetches data from DB and sends to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> stopLiveEnvironment(@RequestParam Integer userId) {
        log.info("Stopping live environment for userId: {}", userId);

        try {
            var liveEnvData = databaseService.getLiveEnvironmentByUserId(userId);

            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("userId", userId);

            if (liveEnvData != null) {
                backendRequest.put("liveEnvId", liveEnvData.get("id"));
                backendRequest.put("dockerContainerId", liveEnvData.get("dockerContainerId"));
            }

            log.info("Sending to Go backend: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/live/stop")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful stop (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        if (liveEnvData != null && liveEnvData.get("id") != null) {
                            return databaseService.updateLiveEnvironmentStatus(
                                (Integer) liveEnvData.get("id"),
                                "stopped"
                            ).thenReturn(ResponseEntity.ok(result));
                        }

                        return Mono.just(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error stopping live environment", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });
        } catch (Exception e) {
            log.error("Error preparing live environment stop", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    @PostMapping("/live/reset")
    @Operation(summary = "Reset Live Environment",
               description = "Frontend sends: userId. API fetches data from DB and sends to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> resetLiveEnvironment(@RequestParam Integer userId) {
        log.info("Resetting live environment for userId: {}", userId);

        try {
            var liveEnvData = databaseService.getLiveEnvironmentByUserId(userId);

            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("userId", userId);

            if (liveEnvData != null) {
                backendRequest.put("liveEnvId", liveEnvData.get("id"));
                backendRequest.put("dockerContainerId", liveEnvData.get("dockerContainerId"));
                backendRequest.put("dockerImage", liveEnvData.get("dockerImage"));
            }

            log.info("Sending to Go backend: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/live/reset")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful reset (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        if (liveEnvData != null && liveEnvData.get("id") != null) {
                            return databaseService.updateLiveEnvironmentStatus(
                                (Integer) liveEnvData.get("id"),
                                "stopped"
                            ).thenReturn(ResponseEntity.ok(result));
                        }

                        return Mono.just(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error resetting live environment", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });
        } catch (Exception e) {
            log.error("Error preparing live environment reset", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    // ==================== IMAGES ====================

    @GetMapping("/images")
    @Operation(summary = "Get All Images", description = "Frontend: no params. API gets all images from DB and returns them.")
    public ResponseEntity<Object> getAllImages() {
        log.info("Getting all images from database");
        try {
            // Hole alle Images aus der Datenbank
            var images = databaseService.getAllImages();
            return ResponseEntity.ok(images);
        } catch (Exception e) {
            log.error("Error getting images from database", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/images/add")
    @Operation(summary = "Add Image",
               description = "Frontend sends: imageRef, name. API saves to DB and sends to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> addImage(@RequestParam String imageRef,
                                                                @RequestParam String name) {
        log.info("Adding image: imageRef={}, name={}", imageRef, name);

        try {
            // Speichere Image zuerst in der Datenbank
            Map<String, Object> dbImage = new HashMap<>();
            dbImage.put("imageRef", imageRef);
            dbImage.put("name", name);

            Map<String, Object> savedImage = databaseService.createImage(dbImage);

            // Sende vollständige Daten ans Go-Backend
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("id", savedImage.get("id"));
            backendRequest.put("imageRef", imageRef);
            backendRequest.put("name", name);

            log.info("Sending image to Go backend: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/images/add")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);
                        result.put("image", savedImage);
                        return ResponseEntity.ok(result);
                    })
                    .onErrorResume(e -> {
                        log.error("Error adding image to backend", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });
        } catch (Exception e) {
            log.error("Error preparing image add", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    @DeleteMapping("/images/remove")
    @Operation(summary = "Remove Image",
               description = "Frontend sends: imageId. API gets image from DB and removes from Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> removeImage(@RequestParam Integer imageId) {
        log.info("Removing image: imageId={}", imageId);

        try {
            // Hole Image-Daten aus Datenbank
            var imageData = databaseService.getImageById(imageId);

            if (imageData == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Image not found with id=" + imageId);
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            // Sende vollständige Daten ans Go-Backend
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("id", imageId);
            backendRequest.put("imageRef", imageData.getImageRef());
            backendRequest.put("name", imageData.getName());

            log.info("Sending image removal to Go backend: {}", backendRequest);

            return backendWebClient.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/images/remove")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(response -> {
                        // Lösche aus Datenbank
                        databaseService.deleteImage(imageId);

                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);
                        return ResponseEntity.ok(result);
                    })
                    .onErrorResume(e -> {
                        log.error("Error removing image from backend", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });
        } catch (Exception e) {
            log.error("Error preparing image removal", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    // ==================== INSTANCES ====================

    @GetMapping("/instances")
    @Operation(summary = "Get All Instances", description = "Frontend: no params. API gets all instances from DB and returns them.")
    public ResponseEntity<Object> getAllInstances() {
        log.info("Getting all instances from database");
        try {
            // Hole alle Instanzen aus der Datenbank
            var instances = databaseService.getAllInstances();
            return ResponseEntity.ok(instances);
        } catch (Exception e) {
            log.error("Error getting instances from database", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/instances/start")
    @Operation(summary = "Start Instance", description = "Frontend sends: userId, imageId. API fetches ALL data from DB and sends EVERYTHING to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> startInstance(@RequestParam Integer userId,
                                                                     @RequestParam Integer imageId) {
        log.info("Starting instance for userId: {}, imageId: {}", userId, imageId);

        try {
            // Hole Instance aus Datenbank (Port 5050)
            InstanceDTO[] instances = databaseService.getInstancesByUserAndImage(userId, imageId);

            if (instances == null || instances.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No instance found for userId=" + userId + " imageId=" + imageId);
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            InstanceDTO instance = instances[0];

            // Baue VOLLSTÄNDIGES Request-Objekt mit ALLEN DB-Daten
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());
            backendRequest.put("name", instance.getName());
            backendRequest.put("userId", instance.getUserId());
            backendRequest.put("imageId", instance.getImageId());
            backendRequest.put("status", instance.getStatus());
            backendRequest.put("id", instance.getId());

            // Füge ALLE Image-Informationen hinzu
            if (instance.getImage() != null) {
                backendRequest.put("imageRef", instance.getImage().getImageRef());
                backendRequest.put("imageName", instance.getImage().getName());
                backendRequest.put("image_id", instance.getImage().getId());
            }

            // Füge ALLE User-Informationen hinzu
            if (instance.getUser() != null) {
                backendRequest.put("userName", instance.getUser().getName());
                backendRequest.put("userEmail", instance.getUser().getEmail());
                backendRequest.put("user_id", instance.getUser().getId());
            }

            log.info("Sending COMPLETE instance data to backend: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/instances/start")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful start (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        return databaseService.updateInstanceStatus(instance.getId(), "running")
                                .thenReturn(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error starting instance", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });

        } catch (Exception e) {
            log.error("Error preparing instance start", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    @PostMapping("/instances/stop")
    @Operation(summary = "Stop Instance", description = "Frontend sends: userId, imageId. API fetches ALL data from DB and sends EVERYTHING to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> stopInstance(@RequestParam Integer userId,
                                                                    @RequestParam Integer imageId) {
        log.info("Stopping instance for userId: {}, imageId: {}", userId, imageId);

        try {
            // Hole Instance aus Datenbank (Port 5050)
            InstanceDTO[] instances = databaseService.getInstancesByUserAndImage(userId, imageId);

            if (instances == null || instances.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No instance found for userId=" + userId + " imageId=" + imageId);
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            InstanceDTO instance = instances[0];

            // Baue VOLLSTÄNDIGES Request-Objekt mit ALLEN DB-Daten
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());
            backendRequest.put("name", instance.getName());
            backendRequest.put("userId", instance.getUserId());
            backendRequest.put("imageId", instance.getImageId());
            backendRequest.put("status", instance.getStatus());
            backendRequest.put("id", instance.getId());

            // Füge ALLE Image-Informationen hinzu
            if (instance.getImage() != null) {
                backendRequest.put("imageRef", instance.getImage().getImageRef());
                backendRequest.put("imageName", instance.getImage().getName());
                backendRequest.put("image_id", instance.getImage().getId());
            }

            // Füge ALLE User-Informationen hinzu
            if (instance.getUser() != null) {
                backendRequest.put("userName", instance.getUser().getName());
                backendRequest.put("userEmail", instance.getUser().getEmail());
                backendRequest.put("user_id", instance.getUser().getId());
            }

            log.info("Sending COMPLETE instance data to backend for stop: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/instances/stop")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful stop (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        return databaseService.updateInstanceStatus(instance.getId(), "stopped")
                                .thenReturn(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error stopping instance", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });

        } catch (Exception e) {
            log.error("Error preparing instance stop", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }

    @PostMapping("/instances/reset")
    @Operation(summary = "Reset Instance", description = "Frontend sends: userId, imageId. API fetches ALL data from DB and sends EVERYTHING to Go backend.")
    public Mono<ResponseEntity<Map<String, Object>>> resetInstance(@RequestParam Integer userId,
                                                                     @RequestParam Integer imageId) {
        log.info("Resetting instance for userId: {}, imageId: {}", userId, imageId);

        try {
            // Hole Instance aus Datenbank (Port 5050)
            InstanceDTO[] instances = databaseService.getInstancesByUserAndImage(userId, imageId);

            if (instances == null || instances.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "No instance found for userId=" + userId + " imageId=" + imageId);
                return Mono.just(ResponseEntity.badRequest().body(error));
            }

            InstanceDTO instance = instances[0];

            // Baue VOLLSTÄNDIGES Request-Objekt mit ALLEN DB-Daten
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());
            backendRequest.put("name", instance.getName());
            backendRequest.put("userId", instance.getUserId());
            backendRequest.put("imageId", instance.getImageId());
            backendRequest.put("status", instance.getStatus());
            backendRequest.put("id", instance.getId());

            // Füge ALLE Image-Informationen hinzu
            if (instance.getImage() != null) {
                backendRequest.put("imageRef", instance.getImage().getImageRef());
                backendRequest.put("imageName", instance.getImage().getName());
                backendRequest.put("image_id", instance.getImage().getId());
            }

            // Füge ALLE User-Informationen hinzu
            if (instance.getUser() != null) {
                backendRequest.put("userName", instance.getUser().getName());
                backendRequest.put("userEmail", instance.getUser().getEmail());
                backendRequest.put("user_id", instance.getUser().getId());
            }

            log.info("Sending COMPLETE instance data to backend for reset: {}", backendRequest);

            return backendWebClient.post()
                    .uri("/instances/reset")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(String.class)
                    .flatMap(response -> {
                        // Update status in database after successful reset (non-blocking)
                        Map<String, Object> result = new HashMap<>();
                        result.put("message", response);

                        return databaseService.updateInstanceStatus(instance.getId(), "stopped")
                                .thenReturn(ResponseEntity.ok(result));
                    })
                    .onErrorResume(e -> {
                        log.error("Error resetting instance", e);
                        Map<String, Object> error = new HashMap<>();
                        error.put("error", e.getMessage());
                        return Mono.just(ResponseEntity.badRequest().body(error));
                    });

        } catch (Exception e) {
            log.error("Error preparing instance reset", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return Mono.just(ResponseEntity.badRequest().body(error));
        }
    }
}
