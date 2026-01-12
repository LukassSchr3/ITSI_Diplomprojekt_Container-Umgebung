package itsi.api.steuerung.service;

import itsi.api.steuerung.dto.ContainerOperationRequest;
import itsi.api.steuerung.dto.ContainerOperationResponse;
import itsi.api.steuerung.dto.ImageDTO;
import itsi.api.steuerung.dto.InstanceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ContainerService {

    private final WebClient backendWebClient;
    private final DatabaseService databaseService;

    public ContainerService(
            @Qualifier("backendWebClient") WebClient backendWebClient,
            DatabaseService databaseService) {
        this.backendWebClient = backendWebClient;
        this.databaseService = databaseService;
    }

    public ContainerOperationResponse startContainer(ContainerOperationRequest request) {
        log.info("Starting container for userId: {}, imageId: {}", request.getUserId(), request.getImageId());

        try {
            // Find or create instance for this user and image
            InstanceDTO instance = databaseService.findOrCreateInstance(request.getUserId(), request.getImageId());
            log.info("Using instance: {} (id: {})", instance.getName(), instance.getId());

            // If instance has no containerId, create and start it (this sets containerId)
            if (instance.getContainerId() == null || instance.getContainerId().isEmpty()) {
                InstanceDTO created = createAndStartContainerIfMissing(request.getUserId(), request.getImageId());
                if (created == null) {
                    return new ContainerOperationResponse(false, "Failed to create/start container", null, null, null);
                }
                return new ContainerOperationResponse(
                        true,
                        "Container created and started successfully",
                        created.getContainerId(),
                        created.getStatus(),
                        created
                );
            }

            // Send start request to backend controller (only containerId)
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());

            Map<String, Object> backendResponse = backendWebClient.post()
                    .uri("/containers/start")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            // Update instance status in database
            instance.setStatus("running");
            InstanceDTO updatedInstance = databaseService.updateInstance(instance.getId(), instance);

            return new ContainerOperationResponse(
                    true,
                    "Container started successfully",
                    instance.getContainerId(),
                    "running",
                    updatedInstance
            );

        } catch (Exception e) {
            log.error("Error starting container", e);
            return new ContainerOperationResponse(
                    false,
                    "Error: " + e.getMessage(),
                    null,
                    null,
                    null
            );
        }
    }

    public ContainerOperationResponse stopContainer(ContainerOperationRequest request) {
        log.info("Stopping container for userId: {}, imageId: {}", request.getUserId(), request.getImageId());

        try {
            // Find instance for this user and image
            InstanceDTO[] instances = databaseService.getInstancesByUserAndImage(request.getUserId(), request.getImageId());
            
            if (instances == null || instances.length == 0) {
                return new ContainerOperationResponse(
                        false,
                        "No instance found for this user and image",
                        null,
                        null,
                        null
                );
            }

            InstanceDTO instance = instances[0];
            log.info("Stopping instance: {} (id: {})", instance.getName(), instance.getId());

            // Send stop request to backend controller (only containerId)
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());

            Map<String, Object> backendResponse = backendWebClient.post()
                    .uri("/containers/stop")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            // Update instance status in database
            instance.setStatus("stopped");
            InstanceDTO updatedInstance = databaseService.updateInstance(instance.getId(), instance);

            return new ContainerOperationResponse(
                    true,
                    "Container stopped successfully",
                    instance.getContainerId(),
                    "stopped",
                    updatedInstance
            );

        } catch (Exception e) {
            log.error("Error stopping container", e);
            return new ContainerOperationResponse(
                    false,
                    "Error: " + e.getMessage(),
                    null,
                    null,
                    null
            );
        }
    }

    public ContainerOperationResponse resetContainer(ContainerOperationRequest request) {
        log.info("Resetting container for userId: {}, imageId: {}", request.getUserId(), request.getImageId());

        try {
            // Find instance for this user and image
            InstanceDTO[] instances = databaseService.getInstancesByUserAndImage(request.getUserId(), request.getImageId());
            
            if (instances == null || instances.length == 0) {
                return new ContainerOperationResponse(
                        false,
                        "No instance found for this user and image",
                        null,
                        null,
                        null
                );
            }

            InstanceDTO instance = instances[0];
            log.info("Resetting instance: {} (id: {})", instance.getName(), instance.getId());

            // Send reset request to backend controller (only containerId)
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());

            Map<String, Object> backendResponse = backendWebClient.post()
                    .uri("/containers/reset")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            // Update instance status in database
            instance.setStatus("running");
            InstanceDTO updatedInstance = databaseService.updateInstance(instance.getId(), instance);

            return new ContainerOperationResponse(
                    true,
                    "Container reset successfully",
                    instance.getContainerId(),
                    "running",
                    updatedInstance
            );

        } catch (Exception e) {
            log.error("Error resetting container", e);
            return new ContainerOperationResponse(
                    false,
                    "Error: " + e.getMessage(),
                    null,
                    null,
                    null
            );
        }
    }

    /**
     * Ensure there is an instance with a containerId for the given userId and imageId.
     * If the instance exists but has no containerId, create a new containerId of the form "cont_<n>"
     * where n is (max existing number) + 1 obtained from the database API, set the instance name to
     * "<imageName>_<userName>", call the backend to create/start the container and update the instance in DB.
     *
     * Returns the updated InstanceDTO (with containerId and updated status) or the existing instance if already had containerId.
     */
    public InstanceDTO createAndStartContainerIfMissing(Integer userId, Integer imageId) {
        log.info("Ensure container for userId: {}, imageId: {}", userId, imageId);

        try {
            // Get or create instance record
            InstanceDTO instance = databaseService.findOrCreateInstance(userId, imageId);
            if (instance == null) {
                log.error("Unable to find or create instance for userId {} imageId {}", userId, imageId);
                return null;
            }

            // If containerId already present, nothing to do
            if (instance.getContainerId() != null && !instance.getContainerId().isEmpty()) {
                log.info("Instance already has containerId {}", instance.getContainerId());
                return instance;
            }

            // Determine next container id from DB
            String maxContainerId = databaseService.getMaxContainerId();
            int next = 1;
            if (maxContainerId != null && !maxContainerId.isEmpty()) {
                try {
                    String numericPart = maxContainerId.replaceAll("^cont_", "");
                    next = Integer.parseInt(numericPart) + 1;
                } catch (NumberFormatException ex) {
                    log.warn("Could not parse maxContainerId='{}'. Defaulting to 1", maxContainerId);
                    next = 1;
                }
            }
            String newContainerId = "cont_" + next;
            instance.setContainerId(newContainerId);

            // Ensure proper name format: imageName_userName
            String imageName = "img";
            if (instance.getImage() != null && instance.getImage().getName() != null) {
                imageName = instance.getImage().getName();
            }
            String userName = "user";
            if (instance.getUser() != null && instance.getUser().getName() != null) {
                userName = instance.getUser().getName();
            }
            instance.setName(imageName + "_" + userName);

            // Call backend to create/start the container
            Map<String, Object> backendRequest = new HashMap<>();
            backendRequest.put("containerId", instance.getContainerId());
            backendRequest.put("imageRef", instance.getImage() != null ? instance.getImage().getImageRef() : null);
            backendRequest.put("name", instance.getName());
            backendRequest.put("userId", userId);

            Map<String, Object> backendResponse = backendWebClient.post()
                    .uri("/containers/start")
                    .bodyValue(backendRequest)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(60))
                    .block();

            log.info("Backend response when creating container: {}", backendResponse);

            // Update instance status and persist
            instance.setStatus("running");
            InstanceDTO updated;
            if (instance.getId() != null) {
                updated = databaseService.updateInstance(instance.getId(), instance);
            } else {
                updated = databaseService.createInstance(instance);
            }

            return updated;

        } catch (Exception e) {
            log.error("Failed to create/start container for userId {} imageId {}", userId, imageId, e);
            return null;
        }
    }
}
