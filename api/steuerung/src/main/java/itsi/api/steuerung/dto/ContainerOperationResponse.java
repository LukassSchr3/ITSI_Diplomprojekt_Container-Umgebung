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
    private String containerIp;

    public ContainerOperationResponse(boolean success, String message, String containerId, String status, InstanceDTO instance) {
        this.success = success;
        this.message = message;
        this.containerId = containerId;
        this.status = status;
        this.instance = instance;
    }
}
