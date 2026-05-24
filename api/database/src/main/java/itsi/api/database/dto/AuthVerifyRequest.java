package itsi.api.database.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthVerifyRequest {
    private String email;
    private String password;
}
