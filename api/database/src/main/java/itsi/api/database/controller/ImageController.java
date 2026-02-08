package itsi.api.database.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import itsi.api.database.entity.Image;
import itsi.api.database.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Tag(name = "Images", description = "Docker Image Management API")
public class ImageController {

    private final ImageService imageService;

    @GetMapping
    @Operation(summary = "Alle Images abrufen", description = "Gibt eine Liste aller Docker Images zurück")
    public ResponseEntity<List<Image>> getAllImages() {
        return ResponseEntity.ok(imageService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Image nach ID abrufen")
    public ResponseEntity<Image> getImageById(@PathVariable Integer id) {
        return imageService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/name/{name}")
    @Operation(summary = "Image nach Namen abrufen")
    public ResponseEntity<Image> getImageByName(@PathVariable String name) {
        return imageService.findByName(name)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ref/{imageRef}")
    @Operation(summary = "Image nach Referenz abrufen")
    public ResponseEntity<Image> getImageByRef(@PathVariable String imageRef) {
        return imageService.findByImageRef(imageRef)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Neues Image erstellen")
    public ResponseEntity<Image> createImage(@RequestBody Image image) {
        Image savedImage = imageService.save(image);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedImage);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Image aktualisieren")
    public ResponseEntity<Image> updateImage(@PathVariable Integer id, @RequestBody Image image) {
        if (!imageService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        image.setId(id);
        return ResponseEntity.ok(imageService.save(image));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Image löschen")
    public ResponseEntity<Void> deleteImage(@PathVariable Integer id) {
        if (!imageService.findById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        imageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
