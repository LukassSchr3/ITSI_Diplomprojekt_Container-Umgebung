package itsi.api.steuerung.service;

import itsi.api.steuerung.dto.ImageDTO;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
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
}
