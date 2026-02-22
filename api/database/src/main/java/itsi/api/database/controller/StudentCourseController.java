package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.StudentCourse;
import itsi.api.database.service.StudentCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
@Tag(name = "Student Courses", description = "Schüler-Kurs Zuordnung API")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;

    @GetMapping
    @Operation(summary = "Alle Zuordnungen abrufen")
    public ResponseEntity<List<StudentCourse>> getAllStudentCourses() {
        return ResponseEntity.ok(studentCourseService.findAll());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Alle Kurse eines Schülers abrufen")
    public ResponseEntity<List<StudentCourse>> getCoursesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(studentCourseService.findByUserId(userId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Alle Schüler eines Kurses abrufen")
    public ResponseEntity<List<StudentCourse>> getStudentsByCourseId(@PathVariable Integer courseId) {
        return ResponseEntity.ok(studentCourseService.findByCourseId(courseId));
    }

    @GetMapping("/user/{userId}/course/{courseId}")
    @Operation(summary = "Spezifische Zuordnung abrufen")
    public ResponseEntity<StudentCourse> getStudentCourse(@PathVariable Integer userId, @PathVariable Integer courseId) {
        return studentCourseService.findByUserIdAndCourseId(userId, courseId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Schüler zu Kurs zuordnen")
    public ResponseEntity<?> enrollStudent(@RequestBody StudentCourse studentCourse) {
        try {
            if (studentCourseService.findByUserIdAndCourseId(studentCourse.getUserId(), studentCourse.getCourseId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Schüler ist bereits in diesem Kurs"));
            }
            StudentCourse saved = studentCourseService.save(studentCourse);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Zuordnen: " + e.getMessage()));
        }
    }

    @DeleteMapping("/user/{userId}/course/{courseId}")
    @Operation(summary = "Schüler aus Kurs entfernen")
    public ResponseEntity<Void> unenrollStudent(@PathVariable Integer userId, @PathVariable Integer courseId) {
        if (!studentCourseService.findByUserIdAndCourseId(userId, courseId).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        studentCourseService.deleteByUserIdAndCourseId(userId, courseId);
        return ResponseEntity.noContent().build();
    }
}
