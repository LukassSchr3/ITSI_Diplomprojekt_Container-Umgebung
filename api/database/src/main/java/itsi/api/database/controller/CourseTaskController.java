package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.CourseTask;
import itsi.api.database.entity.Task;
import itsi.api.database.service.CourseTaskService;
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
@RequestMapping("/api/course-tasks")
@RequiredArgsConstructor
@Tag(name = "Course Tasks", description = "Kurs-Aufgaben Zuordnung API")
public class CourseTaskController {

    private final CourseTaskService courseTaskService;

    @GetMapping
    @Operation(summary = "Alle Zuordnungen abrufen")
    public ResponseEntity<List<CourseTask>> getAllCourseTasks() {
        return ResponseEntity.ok(courseTaskService.findAll());
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Alle Aufgaben-Zuordnungen eines Kurses abrufen (mit Reihenfolge)")
    public ResponseEntity<List<CourseTask>> getTasksByCourseId(@PathVariable Integer courseId) {
        return ResponseEntity.ok(courseTaskService.findByCourseId(courseId));
    }

    @GetMapping("/course/{courseId}/tasks")
    @Operation(summary = "Alle Aufgaben eines Kurses abrufen (komplette Task-Objekte, sortiert)")
    public ResponseEntity<List<Task>> getTaskObjectsByCourseId(@PathVariable Integer courseId) {
        return ResponseEntity.ok(courseTaskService.findTasksByCourseId(courseId));
    }

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Alle Kurse die eine Aufgabe verwenden")
    public ResponseEntity<List<CourseTask>> getCoursesByTaskId(@PathVariable Integer taskId) {
        return ResponseEntity.ok(courseTaskService.findByTaskId(taskId));
    }

    @GetMapping("/course/{courseId}/task/{taskId}")
    @Operation(summary = "Spezifische Zuordnung abrufen")
    public ResponseEntity<CourseTask> getCourseTask(@PathVariable Integer courseId, @PathVariable Integer taskId) {
        return courseTaskService.findByCourseIdAndTaskId(courseId, taskId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Aufgabe zu Kurs zuordnen")
    public ResponseEntity<?> assignTaskToCourse(@RequestBody CourseTask courseTask) {
        try {
            if (courseTaskService.findByCourseIdAndTaskId(courseTask.getCourseId(), courseTask.getTaskId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Aufgabe ist bereits diesem Kurs zugeordnet"));
            }
            CourseTask saved = courseTaskService.save(courseTask);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Zuordnen: " + e.getMessage()));
        }
    }

    @PutMapping("/course/{courseId}/task/{taskId}")
    @Operation(summary = "Zuordnung aktualisieren (z.B. Reihenfolge ändern)")
    public ResponseEntity<?> updateCourseTask(
            @PathVariable Integer courseId, 
            @PathVariable Integer taskId, 
            @RequestBody CourseTask courseTask) {
        
        return courseTaskService.findByCourseIdAndTaskId(courseId, taskId)
                .map(existing -> {
                    courseTask.setCourseId(courseId);
                    courseTask.setTaskId(taskId);
                    return ResponseEntity.ok(courseTaskService.save(courseTask));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/course/{courseId}/task/{taskId}")
    @Operation(summary = "Aufgabe aus Kurs entfernen")
    public ResponseEntity<Void> removeTaskFromCourse(@PathVariable Integer courseId, @PathVariable Integer taskId) {
        if (!courseTaskService.findByCourseIdAndTaskId(courseId, taskId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        courseTaskService.deleteByCourseIdAndTaskId(courseId, taskId);
        return ResponseEntity.noContent().build();
    }
}
