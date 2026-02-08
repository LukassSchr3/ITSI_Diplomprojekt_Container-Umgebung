package itsi.api.database.controller;

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
@Tag(name = "API Info", description = "API Informationen und Übersicht")
public class ApiInfoController {

    @GetMapping("/info")
    @Operation(summary = "API Übersicht", description = "Zeigt alle verfügbaren API-Endpunkte und deren Beschreibung")
    public ResponseEntity<Map<String, Object>> getApiInfo() {
        Map<String, Object> info = new HashMap<>();

        info.put("name", "Database API");
        info.put("version", "1.0.0");
        info.put("port", 5050);
        info.put("swagger_ui", "http://localhost:5050/swagger-ui.html");
        info.put("h2_console", "http://localhost:5050/h2-console");
        info.put("api_docs", "http://localhost:5050/api-docs");

        Map<String, Object> endpoints = new HashMap<>();

        // Users Endpoints
        Map<String, String> users = new HashMap<>();
        users.put("GET /api/users", "Alle Benutzer abrufen");
        users.put("GET /api/users/{id}", "Benutzer nach ID abrufen");
        users.put("GET /api/users/name/{name}", "Benutzer nach Namen abrufen");
        users.put("POST /api/users", "Neuen Benutzer erstellen");
        users.put("PUT /api/users/{id}", "Benutzer aktualisieren");
        users.put("DELETE /api/users/{id}", "Benutzer löschen");
        endpoints.put("Users", users);

        // Images Endpoints
        Map<String, String> images = new HashMap<>();
        images.put("GET /api/images", "Alle Docker-Images abrufen");
        images.put("GET /api/images/{id}", "Image nach ID abrufen");
        images.put("GET /api/images/name/{name}", "Image nach Namen abrufen");
        images.put("GET /api/images/ref/{imageRef}", "Image nach Referenz abrufen");
        images.put("POST /api/images", "Neues Image erstellen");
        images.put("PUT /api/images/{id}", "Image aktualisieren");
        images.put("DELETE /api/images/{id}", "Image löschen");
        endpoints.put("Images", images);

        // Instances Endpoints
        Map<String, String> instances = new HashMap<>();
        instances.put("GET /api/instances", "Alle Container-Instances abrufen");
        instances.put("GET /api/instances/{id}", "Instance nach ID abrufen");
        instances.put("GET /api/instances/container/{containerId}", "Instance nach Container-ID abrufen");
        instances.put("GET /api/instances/name/{name}", "Instance nach Namen abrufen");
        instances.put("GET /api/instances/user/{userId}", "Alle Instances eines Benutzers abrufen");
        instances.put("GET /api/instances/image/{imageId}", "Alle Instances eines Images abrufen");
        instances.put("GET /api/instances/status/{status}", "Instances nach Status abrufen");
        instances.put("POST /api/instances", "Neue Instance erstellen");
        instances.put("PUT /api/instances/{id}", "Instance aktualisieren");
        instances.put("DELETE /api/instances/{id}", "Instance löschen");
        endpoints.put("Instances", instances);

        info.put("endpoints", endpoints);

        return ResponseEntity.ok(info);
    }

    @GetMapping("/health")
    @Operation(summary = "Health Check", description = "Überprüft ob die API läuft")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("message", "Database API is running");
        return ResponseEntity.ok(health);
    }
}

