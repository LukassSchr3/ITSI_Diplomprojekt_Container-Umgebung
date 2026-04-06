package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Course;
import itsi.api.database.service.CourseService;
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
@RequestMapping("/api/courses")
@RequiredArgsConstructor
@Tag(name = "Courses", description = "Kurs/Semester Management API")
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    @Operation(summary = "Alle Kurse abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<List<Course>> getAllCourses() {
        return ResponseEntity.ok(courseService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Kurs nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Course> getCourseById(@PathVariable Integer id) {
        return courseService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Kurs nach Namen abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Course> getCourseByName(@PathVariable String name) {
        return courseService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Neuen Kurs erstellen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<?> createCourse(@RequestBody Course course) {
        try {
            if (courseService.findByName(course.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Kurs mit Name '" + course.getName() + "' existiert bereits"));
            }
            Course savedCourse = courseService.save(course);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCourse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Erstellen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Kurs aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<Course> updateCourse(@PathVariable Integer id, @RequestBody Course course) {
        if (!courseService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        course.setId(id);
        return ResponseEntity.ok(courseService.save(course));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Kurs löschen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCourse(@PathVariable Integer id) {
        if (!courseService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        courseService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
