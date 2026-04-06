package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Task;
import itsi.api.database.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Aufgaben Management API")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Alle Aufgaben abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<List<Task>> getAllTasks() {
        return ResponseEntity.ok(taskService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Aufgabe nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Task> getTaskById(@PathVariable Integer id) {
        return taskService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "Alle Aufgaben mit einem bestimmten Image abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<List<Task>> getTasksByImageId(@PathVariable Integer imageId) {
        return ResponseEntity.ok(taskService.findByImageId(imageId));
    }

    @PostMapping
    @Operation(summary = "Neue Aufgabe erstellen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        try {
            Task savedTask = taskService.save(task);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedTask);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Erstellen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aufgabe aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<Task> updateTask(@PathVariable Integer id, @RequestBody Task task) {
        if (!taskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        task.setId(id);
        return ResponseEntity.ok(taskService.save(task));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Aufgabe löschen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTask(@PathVariable Integer id) {
        if (!taskService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        taskService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
