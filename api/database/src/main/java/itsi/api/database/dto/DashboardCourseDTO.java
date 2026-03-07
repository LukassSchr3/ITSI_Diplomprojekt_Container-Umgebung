package itsi.api.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardCourseDTO {

    private Integer id;
    private String name;
    private String description;
    private LocalDateTime enrolledAt;
    private LocalDateTime expiresAt;
    private List<TaskDTO> tasks;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaskDTO {
        private Integer id;
        private String title;
        private String description;
        private Integer points;
        private Integer imageId;
    }
}

