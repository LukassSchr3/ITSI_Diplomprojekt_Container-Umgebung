package itsi.api.database.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * Data Transfer Object for updating an existing User
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserDTO {
    private String name;
    private String email;
    private String password; // Optional - nur wenn Passwort geändert werden soll
    private String className;
    private String role;
    private Timestamp expiredAt;
}

