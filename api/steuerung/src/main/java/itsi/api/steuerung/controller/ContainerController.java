package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.steuerung.dto.ContainerOperationRequest;
import itsi.api.steuerung.dto.ContainerOperationResponse;
import itsi.api.steuerung.service.ContainerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/container")
@Tag(name = "Container Operations", description = "Container start, stop, and reset operations")
@Slf4j
public class ContainerController {

    private final ContainerService containerService;

    public ContainerController(ContainerService containerService) {
        this.containerService = containerService;
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
}
