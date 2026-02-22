package itsi.api.database.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "instances")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Instance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "container_id", unique = true, nullable = false)
    private String containerId;

    @Column(unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "image_id", nullable = false)
    private Image image;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "task_id")
    private Integer taskId;

    @Column(name = "course_id")
    private Integer courseId;

    @Column(columnDefinition = "VARCHAR(50) DEFAULT 'created'")
    private String status = "created";
}

