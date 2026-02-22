package itsi.api.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_courses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(StudentCourse.StudentCourseId.class)
public class StudentCourse {

    @Id
    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Id
    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "enrolled_at")
    private LocalDateTime enrolledAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        enrolledAt = LocalDateTime.now();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentCourseId implements Serializable {
        private Integer userId;
        private Integer courseId;
    }
}
