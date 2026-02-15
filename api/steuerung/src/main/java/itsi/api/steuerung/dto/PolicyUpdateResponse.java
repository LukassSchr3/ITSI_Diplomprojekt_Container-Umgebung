package itsi.api.steuerung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PolicyUpdateResponse {
    private boolean success;
    private String message;
    private String error;
}

