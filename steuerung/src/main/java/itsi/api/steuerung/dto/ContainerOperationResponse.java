package itsi.api.steuerung.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContainerOperationResponse {
    private boolean success;
    private String message;
    private String containerId;
    private String status;
    private InstanceDTO instance;
}
