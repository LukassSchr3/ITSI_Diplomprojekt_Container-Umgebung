package itsi.api.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object for creating a new User
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserDTO {
    private String name;
    private String email;
    private String password;
    private String className;
    private String role;
    private Timestamp expiredAt;
}

