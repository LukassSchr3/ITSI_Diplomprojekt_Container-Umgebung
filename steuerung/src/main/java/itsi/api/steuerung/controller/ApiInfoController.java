package itsi.api.steuerung.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "API Info", description = "API Information and Overview")
public class ApiInfoController {

    @GetMapping("/info")
    @Operation(summary = "API Overview", description = "Shows all available API endpoints and their descriptions")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("name", "Container Steuerung Middleware API");
        info.put("version", "1.0.0");
        info.put("port", 8080);
        info.put("description", "Middleware API for container operations - connects frontend to database (5050) and backend controller (3030)");
        info.put("swagger_ui", "http://localhost:8080/swagger-ui.html");
        info.put("api_docs", "http://localhost:8080/api-docs");

        Map<String, Object> endpoints = new HashMap<>();

        // Container Operations Endpoints
        Map<String, String> container = new HashMap<>();
        container.put("POST /api/container/start", "Start a container");
        container.put("POST /api/container/stop", "Stop a container");
        container.put("POST /api/container/reset", "Reset a container");
        endpoints.put("Container Operations", container);

        info.put("endpoints", endpoints);

        Map<String, String> connections = new HashMap<>();
        connections.put("database_api", "http://localhost:5050");
        connections.put("backend_controller", "http://localhost:3030");
        info.put("connections", connections);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Checks if the API is running")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "Steuerung Middleware API is running");
        return ResponseEntity.ok(health);
    }
}
