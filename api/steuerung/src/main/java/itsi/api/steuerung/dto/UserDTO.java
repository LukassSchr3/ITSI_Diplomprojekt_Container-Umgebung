package itsi.api.steuerung.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public class UserDTO {
    private Integer id;
    private String name;
    private String email;
    private String password;  // Added for authentication
    private String className;
    private String role;
    private Timestamp createdAt;
    private Timestamp expiredAt;
}
