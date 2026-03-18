package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.dto.DashboardCourseDTO;
import itsi.api.database.dto.EnrollClassDTO;
import itsi.api.database.entity.StudentCourse;
import itsi.api.database.entity.User;
import itsi.api.database.service.StudentCourseService;
import itsi.api.database.service.UserService;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-courses")
@RequiredArgsConstructor
@Tag(name = "Student Courses", description = "Schüler-Kurs Zuordnung API")
public class StudentCourseController {

    private final StudentCourseService studentCourseService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Alle Zuordnungen abrufen")
    public ResponseEntity<List<StudentCourse>> getAllStudentCourses() {
        return ResponseEntity.ok(studentCourseService.findAll());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Alle Kurse eines Schülers abrufen")
    public ResponseEntity<List<StudentCourse>> getCoursesByUserId(@PathVariable String userId) {
        try {
            Integer userIdInt = Integer.parseInt(userId);
            return ResponseEntity.ok(studentCourseService.findByUserId(userIdInt));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(List.of());
        }
    }

    @GetMapping("/user/{userId}/dashboard")
    @Operation(summary = "Dashboard-Daten eines Schülers abrufen (optimiert)")
    public ResponseEntity<List<DashboardCourseDTO>> getDashboardCoursesByUserId(@PathVariable String userId) {
        try {
            Integer userIdInt = Integer.parseInt(userId);
            return ResponseEntity.ok(studentCourseService.getDashboardCoursesByUserId(userIdInt));
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(List.of());
        }
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

    @PostMapping("/enroll-class")
    @Operation(summary = "Alle User einer Klasse in einen Kurs einschreiben")
    public ResponseEntity<?> enrollClass(@RequestBody EnrollClassDTO dto) {
        try {
            List<User> users = userService.findByClassName(dto.getClassName());
            if (users.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Keine Benutzer mit Klasse '" + dto.getClassName() + "' gefunden"));
            }
            List<Integer> enrolled = new ArrayList<>();
            List<Integer> skipped = new ArrayList<>();
            for (User user : users) {
                if (studentCourseService.findByUserIdAndCourseId(user.getId(), dto.getCourseId()).isPresent()) {
                    skipped.add(user.getId());
                } else {
                    StudentCourse sc = new StudentCourse();
                    sc.setUserId(user.getId());
                    sc.setCourseId(dto.getCourseId());
                    sc.setExpiresAt(dto.getExpiresAt());
                    studentCourseService.save(sc);
                    enrolled.add(user.getId());
                }
            }
            return ResponseEntity.ok(Map.of(
                    "enrolled", enrolled.size(),
                    "skipped", skipped.size(),
                    "message", enrolled.size() + " Benutzer eingeschrieben, " + skipped.size() + " bereits vorhanden"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Fehler beim Einschreiben: " + e.getMessage()));
        }
    }
}
