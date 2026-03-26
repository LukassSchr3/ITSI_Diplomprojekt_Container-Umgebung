package itsi.api.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "course_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@IdClass(CourseTask.CourseTaskId.class)
public class CourseTask {

    @Id
    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Id
    @Column(name = "task_id", nullable = false)
    private Integer taskId;

    @Column(name = "order_index")
    private Integer orderIndex = 0;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseTaskId implements Serializable {
        private Integer courseId;
        private Integer taskId;
    }
}
