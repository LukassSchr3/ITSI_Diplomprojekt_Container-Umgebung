package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Instance;
import itsi.api.database.service.InstanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
@RequiredArgsConstructor
@Tag(name = "Instances", description = "Container Instance Management API")
public class InstanceController {

    private final InstanceService instanceService;

    @GetMapping
    @Operation(summary = "Alle Instances abrufen", description = "Gibt eine Liste aller Container-Instances zurück")
    public ResponseEntity<List<Instance>> getAllInstances() {
        return ResponseEntity.ok(instanceService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Instance nach ID abrufen")
    public ResponseEntity<Instance> getInstanceById(@PathVariable Integer id) {
        return instanceService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/container/{containerId}")
    @Operation(summary = "Instance nach Container-ID abrufen")
    public ResponseEntity<Instance> getInstanceByContainerId(@PathVariable String containerId) {
        return instanceService.findByContainerId(containerId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Instance nach Namen abrufen")
    public ResponseEntity<Instance> getInstanceByName(@PathVariable String name) {
        return instanceService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Alle Instances eines Benutzers abrufen")
    public ResponseEntity<List<Instance>> getInstancesByUserId(@PathVariable Integer userId) {
        return ResponseEntity.ok(instanceService.findByUserId(userId));
    }

    @GetMapping("/image/{imageId}")
    @Operation(summary = "Alle Instances eines Images abrufen")
    public ResponseEntity<List<Instance>> getInstancesByImageId(@PathVariable Integer imageId) {
        return ResponseEntity.ok(instanceService.findByImageId(imageId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Alle Instances nach Status abrufen")
    public ResponseEntity<List<Instance>> getInstancesByStatus(@PathVariable String status) {
        return ResponseEntity.ok(instanceService.findByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Neue Instance erstellen")
    public ResponseEntity<Instance> createInstance(@RequestBody Instance instance) {
        Instance savedInstance = instanceService.save(instance);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedInstance);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Instance aktualisieren")
    public ResponseEntity<Instance> updateInstance(@PathVariable Integer id, @RequestBody Instance instance) {
        if (!instanceService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        instance.setId(id);
        return ResponseEntity.ok(instanceService.save(instance));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Instance löschen")
    public ResponseEntity<Void> deleteInstance(@PathVariable Integer id) {
        if (!instanceService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        instanceService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/max-container-id")
    @Operation(summary = "Größte Container-ID abrufen")
    public ResponseEntity<String> getMaxContainerId() {
        return instanceService.findMaxContainerId()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
