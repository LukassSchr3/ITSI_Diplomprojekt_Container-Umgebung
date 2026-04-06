package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Image;
import itsi.api.database.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Docker Image Management API")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "Alle Images abrufen", description = "Gibt eine Liste aller Docker Images zurück")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<List<Image>> getAllImages() {
        return ResponseEntity.ok(imageService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Image nach ID abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Image> getImageById(@PathVariable Integer id) {
        return imageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Image nach Namen abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Image> getImageByName(@PathVariable String name) {
        return imageService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ref/{imageRef}")
    @Operation(summary = "Image nach Referenz abrufen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER', 'SCHUELER')")
    public ResponseEntity<Image> getImageByRef(@PathVariable String imageRef) {
        return imageService.findByImageRef(imageRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Neues Image erstellen")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<?> createImage(@RequestBody Image image) {
        try {
            // Check if image with same name already exists
            if (imageService.findByName(image.getName()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Image with name '" + image.getName() + "' already exists"));
            }
            
            // Check if image with same imageRef already exists
            if (imageService.findByImageRef(image.getImageRef()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error", "Image with imageRef '" + image.getImageRef() + "' already exists"));
            }
            
            Image savedImage = imageService.save(image);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedImage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Failed to create image: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Image aktualisieren")
    @PreAuthorize("hasAnyRole('ADMIN', 'LEHRER')")
    public ResponseEntity<Image> updateImage(@PathVariable Integer id, @RequestBody Image image) {
        if (!imageService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        image.setId(id);
        return ResponseEntity.ok(imageService.save(image));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Image löschen")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer id) {
        if (!imageService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        imageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
