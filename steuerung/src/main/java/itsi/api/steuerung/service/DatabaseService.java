package itsi.api.steuerung.service;

import itsi.api.steuerung.dto.ImageDTO;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class DatabaseService {

    private final WebClient databaseWebClient;

    public DatabaseService(@Qualifier("databaseWebClient") WebClient databaseWebClient) {
        this.databaseWebClient = databaseWebClient;
    }

    public InstanceDTO getInstanceById(Integer id) {
        log.debug("Fetching instance with id: {}", id);
        return databaseWebClient.get()
                .uri("/api/instances/{id}", id)
                .retrieve()
                .bodyToMono(InstanceDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public InstanceDTO getInstanceByContainerId(String containerId) {
        log.debug("Fetching instance with containerId: {}", containerId);
        return databaseWebClient.get()
                .uri("/api/instances/container/{containerId}", containerId)
                .retrieve()
                .bodyToMono(InstanceDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public InstanceDTO createInstance(InstanceDTO instance) {
        log.debug("Creating instance: {}", instance);
        return databaseWebClient.post()
                .uri("/api/instances")
                .bodyValue(instance)
                .retrieve()
                .bodyToMono(InstanceDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public InstanceDTO updateInstance(Integer id, InstanceDTO instance) {
        log.debug("Updating instance with id: {}", id);
        return databaseWebClient.put()
                .uri("/api/instances/{id}", id)
                .bodyValue(instance)
                .retrieve()
                .bodyToMono(InstanceDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public UserDTO getUserById(Integer id) {
        log.debug("Fetching user with id: {}", id);
        return databaseWebClient.get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(UserDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public ImageDTO getImageById(Integer id) {
        log.debug("Fetching image with id: {}", id);
        return databaseWebClient.get()
                .uri("/api/images/{id}", id)
                .retrieve()
                .bodyToMono(ImageDTO.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public ImageDTO[] getAllImages() {
        log.debug("Fetching all images from database");
        return databaseWebClient.get()
                .uri("/api/images")
                .retrieve()
                .bodyToMono(ImageDTO[].class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public InstanceDTO[] getAllInstances() {
        log.debug("Fetching all instances from database");
        return databaseWebClient.get()
                .uri("/api/instances")
                .retrieve()
                .bodyToMono(InstanceDTO[].class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public void deleteInstance(Integer id) {
        log.debug("Deleting instance with id: {}", id);
        databaseWebClient.delete()
                .uri("/api/instances/{id}", id)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public String getMaxContainerId() {
        log.debug("Fetching max container ID");
        String maxId = databaseWebClient.get()
                .uri("/api/instances/max-container-id")
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .block();
        
        log.debug("Max container ID: {}", maxId);
        return maxId;
    }

    public InstanceDTO[] getInstancesByUserAndImage(Integer userId, Integer imageId) {
        log.debug("Fetching instances for userId: {} and imageId: {}", userId, imageId);
        // Get all instances for the user
        InstanceDTO[] userInstances = databaseWebClient.get()
                .uri("/api/instances/user/{userId}", userId)
                .retrieve()
                .bodyToMono(InstanceDTO[].class)
                .timeout(Duration.ofSeconds(30))
                .block();

        // Filter by imageId
        if (userInstances == null) {
            return new InstanceDTO[0];
        }

        return java.util.Arrays.stream(userInstances)
                .filter(instance -> instance.getImageId() != null && instance.getImageId().equals(imageId))
                .toArray(InstanceDTO[]::new);
    }

    public InstanceDTO findOrCreateInstance(Integer userId, Integer imageId) {
        log.debug("Finding or creating instance for userId: {} and imageId: {}", userId, imageId);
        
        // Check if instance already exists
        InstanceDTO[] existingInstances = getInstancesByUserAndImage(userId, imageId);
        
        if (existingInstances != null && existingInstances.length > 0) {
            log.info("Found existing instance: {}", existingInstances[0].getId());
            return existingInstances[0];
        }
        
        // Create new instance
        log.info("No existing instance found. Creating new instance for userId: {} and imageId: {}", userId, imageId);
        
        UserDTO user = getUserById(userId);
        ImageDTO image = getImageById(imageId);
        
        InstanceDTO newInstance = new InstanceDTO();
        newInstance.setUser(user);
        newInstance.setImage(image);
        newInstance.setName(user.getName() + "_" + image.getName());
        newInstance.setContainerId(""); // Will be set by backend
        newInstance.setStatus("created");
        
        return createInstance(newInstance);
    }

    public java.util.Map<String, Object> getLiveEnvironmentByUserId(Integer userId) {
        log.debug("Fetching live-environment for userId: {}", userId);
        try {
            return databaseWebClient.get()
                    .uri("/api/live-environments/{userId}", userId)
                    .retrieve()
                    .bodyToMono(java.util.Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();
        } catch (Exception e) {
            log.warn("No live-environment found for userId: {}", userId);
            return null;
        }
    }

    public java.util.Map<String, Object> createImage(java.util.Map<String, Object> image) {
        log.debug("Creating image in database: {}", image);
        return databaseWebClient.post()
                .uri("/api/images")
                .bodyValue(image)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public void deleteImage(Integer imageId) {
        log.debug("Deleting image with id: {}", imageId);
        databaseWebClient.delete()
                .uri("/api/images/{id}", imageId)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(Duration.ofSeconds(30))
                .block();
    }

    public Mono<Void> updateInstanceStatus(Integer instanceId, String status) {
        log.debug("Updating instance {} status to: {}", instanceId, status);

        return databaseWebClient.get()
                .uri("/api/instances/{id}", instanceId)
                .retrieve()
                .bodyToMono(InstanceDTO.class)
                .flatMap(instance -> {
                    instance.setStatus(status);
                    return databaseWebClient.put()
                            .uri("/api/instances/{id}", instanceId)
                            .bodyValue(instance)
                            .retrieve()
                            .bodyToMono(InstanceDTO.class);
                })
                .doOnSuccess(result -> log.info("Instance {} status updated to: {}", instanceId, status))
                .doOnError(e -> log.error("Failed to update instance {} status", instanceId, e))
                .then()
                .onErrorResume(e -> Mono.empty());
    }

    public Mono<Void> updateLiveEnvironmentStatus(Integer liveEnvId, String status) {
        log.debug("Updating live-environment {} status to: {}", liveEnvId, status);

        java.util.Map<String, Object> updateData = new java.util.HashMap<>();
        updateData.put("status", status);

        return databaseWebClient.put()
                .uri("/api/live-environments/{id}", liveEnvId)
                .bodyValue(updateData)
                .retrieve()
                .bodyToMono(java.util.Map.class)
                .timeout(Duration.ofSeconds(30))
                .doOnSuccess(result -> log.info("Live-environment {} status updated to: {}", liveEnvId, status))
                .doOnError(e -> log.error("Failed to update live-environment {} status", liveEnvId, e))
                .then()
                .onErrorResume(e -> Mono.empty());
    }
}
