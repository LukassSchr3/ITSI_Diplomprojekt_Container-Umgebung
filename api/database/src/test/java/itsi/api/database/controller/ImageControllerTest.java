package itsi.api.database.controller;

import itsi.api.database.entity.Image;
import itsi.api.database.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageControllerTest {

    @Mock
    private ImageService imageService;

    @InjectMocks
    private ImageController imageController;

    private Image testImage;

    @BeforeEach
    void setUp() {
        testImage = new Image();
        testImage.setId(1);
        testImage.setName("ubuntu-test");
        testImage.setImageRef("ubuntu:22.04");
    }

    @Test
    void getAllImagesShouldReturnList() {
        when(imageService.findAll()).thenReturn(Arrays.asList(testImage));

        ResponseEntity<List<Image>> response = imageController.getAllImages();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllImagesShouldReturnEmptyList() {
        when(imageService.findAll()).thenReturn(Collections.emptyList());

        assertTrue(imageController.getAllImages().getBody().isEmpty());
    }

    @Test
    void getImageByIdShouldReturnImageWhenExists() {
        when(imageService.findById(1)).thenReturn(Optional.of(testImage));

        ResponseEntity<Image> response = imageController.getImageById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testImage, response.getBody());
    }

    @Test
    void getImageByIdShouldReturn404WhenNotExists() {
        when(imageService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, imageController.getImageById(99).getStatusCode());
    }

    @Test
    void getImageByNameShouldReturnImageWhenExists() {
        when(imageService.findByName("ubuntu-test")).thenReturn(Optional.of(testImage));

        ResponseEntity<Image> response = imageController.getImageByName("ubuntu-test");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testImage, response.getBody());
    }

    @Test
    void getImageByNameShouldReturn404WhenNotExists() {
        when(imageService.findByName("unknown")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, imageController.getImageByName("unknown").getStatusCode());
    }

    @Test
    void getImageByRefShouldReturnImageWhenExists() {
        when(imageService.findByImageRef("ubuntu:22.04")).thenReturn(Optional.of(testImage));

        ResponseEntity<Image> response = imageController.getImageByRef("ubuntu:22.04");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testImage, response.getBody());
    }

    @Test
    void getImageByRefShouldReturn404WhenNotExists() {
        when(imageService.findByImageRef("unknown:latest")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, imageController.getImageByRef("unknown:latest").getStatusCode());
    }

    @Test
    void createImageShouldReturnCreatedWhenNew() {
        when(imageService.findByName("ubuntu-test")).thenReturn(Optional.empty());
        when(imageService.findByImageRef("ubuntu:22.04")).thenReturn(Optional.empty());
        when(imageService.save(any(Image.class))).thenReturn(testImage);

        ResponseEntity<?> response = imageController.createImage(testImage);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testImage, response.getBody());
    }

    @Test
    void createImageShouldReturnConflictWhenNameExists() {
        when(imageService.findByName("ubuntu-test")).thenReturn(Optional.of(testImage));

        ResponseEntity<?> response = imageController.createImage(testImage);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(imageService, never()).save(any());
    }

    @Test
    void createImageShouldReturnConflictWhenRefExists() {
        when(imageService.findByName("ubuntu-test")).thenReturn(Optional.empty());
        when(imageService.findByImageRef("ubuntu:22.04")).thenReturn(Optional.of(testImage));

        ResponseEntity<?> response = imageController.createImage(testImage);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        verify(imageService, never()).save(any());
    }

    @Test
    void updateImageShouldReturnUpdated() {
        when(imageService.findById(1)).thenReturn(Optional.of(testImage));
        when(imageService.save(any(Image.class))).thenReturn(testImage);

        ResponseEntity<Image> response = imageController.updateImage(1, testImage);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testImage, response.getBody());
    }

    @Test
    void updateImageShouldReturn404WhenNotExists() {
        when(imageService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, imageController.updateImage(99, testImage).getStatusCode());
    }

    @Test
    void deleteImageShouldReturnNoContent() {
        when(imageService.findById(1)).thenReturn(Optional.of(testImage));

        ResponseEntity<Void> response = imageController.deleteImage(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(imageService).deleteById(1);
    }

    @Test
    void deleteImageShouldReturn404WhenNotExists() {
        when(imageService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, imageController.deleteImage(99).getStatusCode());
        verify(imageService, never()).deleteById(any());
    }
}

