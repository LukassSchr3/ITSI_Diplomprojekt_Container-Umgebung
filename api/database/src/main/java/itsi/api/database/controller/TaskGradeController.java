package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.TaskGrade;
import itsi.api.database.service.TaskGradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/task-grades")
@RequiredArgsConstructor
@Tag(name = "Task Grades", description = "Aufgaben Benotung API")
public class TaskGradeController {

    private final TaskGradeService taskGradeService;

    @GetMapping
    @Operation(summary = "Alle Benotungen abrufen")
    public ResponseEntity<List<TaskGrade>> getAllTaskGrades() {
        return ResponseEntity.ok(taskGradeService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Benotung nach ID abrufen")
    public ResponseEntity<TaskGrade> getTaskGradeById(@PathVariable Integer id) {
        return taskGradeService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Alle Benotungen eines Schülers abrufen")
    public ResponseEntity<List<TaskGrade>> getGradesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(taskGradeService.findByUserId(userId));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Alle Benotungen einer Aufgabe abrufen")
    public ResponseEntity<List<TaskGrade>> getGradesByTaskId(@PathVariable Integer taskId) {
        return ResponseEntity.ok(taskGradeService.findByTaskId(taskId));
    }

    @GetMapping("/user/{userId}/task/{taskId}")
    @Operation(summary = "Benotung eines Schülers für eine Aufgabe abrufen")
    public ResponseEntity<TaskGrade> getGradeByUserAndTask(@PathVariable Integer userId, @PathVariable Integer taskId) {
        return taskGradeService.findByUserIdAndTaskId(userId, taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/passed/{passed}")
    @Operation(summary = "Benotungen nach Bestanden-Status filtern")
    public ResponseEntity<List<TaskGrade>> getGradesByPassed(@PathVariable Boolean passed) {
        return ResponseEntity.ok(taskGradeService.findByPassed(passed));
    }

    @PostMapping
    @Operation(summary = "Neue Benotung erstellen")
    public ResponseEntity<?> createTaskGrade(@RequestBody TaskGrade taskGrade) {
        try {
            if (taskGradeService.findByUserIdAndTaskId(taskGrade.getUserId(), taskGrade.getTaskId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Benotung für diesen Schüler und diese Aufgabe existiert bereits"));
            }
            TaskGrade saved = taskGradeService.save(taskGrade);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Erstellen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Benotung aktualisieren (nach ID)")
    public ResponseEntity<TaskGrade> updateTaskGrade(@PathVariable Integer id, @RequestBody TaskGrade taskGrade) {
        if (!taskGradeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        taskGrade.setId(id);
        return ResponseEntity.ok(taskGradeService.save(taskGrade));
    }

    @PutMapping("/user/{userId}/task/{taskId}")
    @Operation(summary = "Benotung aktualisieren (nach User + Task)")
    public ResponseEntity<TaskGrade> updateTaskGradeByUserAndTask(
            @PathVariable Integer userId, 
            @PathVariable Integer taskId, 
            @RequestBody TaskGrade taskGrade) {
        
        return taskGradeService.findByUserIdAndTaskId(userId, taskId)
                .map(existing -> {
                    taskGrade.setId(existing.getId());
                    taskGrade.setUserId(userId);
                    taskGrade.setTaskId(taskId);
                    return ResponseEntity.ok(taskGradeService.save(taskGrade));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Benotung löschen")
    public ResponseEntity<Void> deleteTaskGrade(@PathVariable Integer id) {
        if (!taskGradeService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        taskGradeService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
