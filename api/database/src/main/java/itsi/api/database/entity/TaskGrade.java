package itsi.api.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_grades", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "task_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(nullable = false, length = 10)
    private String grade;

    @Column(nullable = false)
    private Boolean passed = false;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at")
    private LocalDateTime gradedAt;

    @PrePersist
    protected void onCreate() {
        gradedAt = LocalDateTime.now();
    }
}
