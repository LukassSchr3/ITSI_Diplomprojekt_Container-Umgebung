package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.steuerung.dto.ContainerOperationRequest;
import itsi.api.steuerung.dto.ContainerOperationResponse;
import itsi.api.steuerung.service.ContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.util.MultiValueMap;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Map;

@RestController
@RequestMapping("/api/container")
@Tag(name = "Container Operations", description = "Container start, stop, and reset operations")
@Slf4j
public class ContainerController {

    private final ContainerService containerService;
    private final WebClient backendWebClient;

    public ContainerController(ContainerService containerService, @Qualifier("backendWebClient") WebClient backendWebClient) {
        this.containerService = containerService;
        this.backendWebClient = backendWebClient;
    }

    @PostMapping("/start")
    @Operation(summary = "Start Container", description = "Starts a container and updates the database")
    public ResponseEntity<ContainerOperationResponse> startContainer(
            @RequestBody ContainerOperationRequest request) {
        log.info("Received start request: {}", request);
        ContainerOperationResponse response = containerService.startContainer(request);
        return response.isSuccess() ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/stop")
    @Operation(summary = "Stop Container", description = "Stops a container and updates the database")
    public ResponseEntity<ContainerOperationResponse> stopContainer(
            @RequestBody ContainerOperationRequest request) {
        log.info("Received stop request: {}", request);
        ContainerOperationResponse response = containerService.stopContainer(request);
        return response.isSuccess() ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset Container", description = "Resets a container (stops and starts fresh) and updates the database")
    public ResponseEntity<ContainerOperationResponse> resetContainer(
            @RequestBody ContainerOperationRequest request) {
        log.info("Received reset request: {}", request);
        ContainerOperationResponse response = containerService.resetContainer(request);
        return response.isSuccess() ? 
                ResponseEntity.ok(response) : 
                ResponseEntity.badRequest().body(response);
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload File to Container", description = "Uploads a file for a container and forwards it to the backend or stores it.")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam("imageId") String imageId,
            @RequestParam Map<String, String> allParams) {
        log.info("Received file upload: {} ({} bytes) for userId={} imageId={}", file.getOriginalFilename(), file.getSize(), userId, imageId);
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        try {
            // Prüfe, ob Instanz für userId und imageId existiert
            String instanceCheckUrl = String.format("/api/instances/user/%s/image/%s", userId, imageId);
            Map<String, Object> instance = backendWebClient.get()
                    .uri(instanceCheckUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            if (instance == null || instance.get("id") == null) {
                // Instanz existiert nicht, also erstellen
                Map<String, Object> newInstance = Map.of(
                        "userId", userId,
                        "imageId", imageId
                );
                instance = backendWebClient.post()
                        .uri("/api/instances")
                        .bodyValue(newInstance)
                        .retrieve()
                        .bodyToMono(Map.class)
                        .block();
                log.info("Neue Instanz erstellt: {}", instance);
            }
            // Instanzdaten ggf. an den Upload anhängen
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", file.getResource());
            builder.part("userId", userId);
            builder.part("imageId", imageId);
            builder.part("instanceId", instance.get("id").toString());
            // Alle weiteren Parameter anhängen (außer file, userId, imageId, instanceId)
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (!"file".equals(entry.getKey()) && !"userId".equals(entry.getKey()) && !"imageId".equals(entry.getKey()) && !"instanceId".equals(entry.getKey())) {
                    builder.part(entry.getKey(), entry.getValue());
                }
            }
            @SuppressWarnings("unchecked")
            MultiValueMap<String, Object> multipartData = (MultiValueMap<String, Object>) (MultiValueMap<?, ?>) builder.build();
            String backendResponse = backendWebClient.post()
                    .uri("/containers/upload")
                    .body(BodyInserters.fromMultipartData(multipartData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return ResponseEntity.ok("Backend response: " + backendResponse);
        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File upload failed: " + e.getMessage());
        }
    }
}
