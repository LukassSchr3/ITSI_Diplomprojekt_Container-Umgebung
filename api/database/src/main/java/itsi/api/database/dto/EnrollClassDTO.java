package itsi.api.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EnrollClassDTO {

    private String className;
    private Integer courseId;
    private LocalDateTime expiresAt;
}

