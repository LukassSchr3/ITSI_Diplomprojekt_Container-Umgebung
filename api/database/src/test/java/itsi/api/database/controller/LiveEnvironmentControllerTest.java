package itsi.api.database.controller;

import itsi.api.database.entity.LiveEnvironment;
import itsi.api.database.service.LiveEnvironmentService;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LiveEnvironmentControllerTest {

    @Mock
    private LiveEnvironmentService service;

    @InjectMocks
    private LiveEnvironmentController liveEnvironmentController;

    private LiveEnvironment testEnv;

    @BeforeEach
    void setUp() {
        testEnv = new LiveEnvironment();
        testEnv.setId(1L);
        testEnv.setUserId(10L);
        testEnv.setVncPort(5900);
        testEnv.setVncHost("localhost");
        testEnv.setVncPassword("secret");
        testEnv.setStatus("running");
    }

    @Test
    void getAllShouldReturnList() {
        when(service.findAll()).thenReturn(Arrays.asList(testEnv));

        List<LiveEnvironment> result = liveEnvironmentController.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(service).findAll();
    }

    @Test
    void getAllShouldReturnEmptyList() {
        when(service.findAll()).thenReturn(Collections.emptyList());

        assertTrue(liveEnvironmentController.getAll().isEmpty());
    }

    @Test
    void getByIdShouldReturnWhenExists() {
        when(service.findById(1L)).thenReturn(Optional.of(testEnv));

        ResponseEntity<LiveEnvironment> response = liveEnvironmentController.getById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testEnv, response.getBody());
    }

    @Test
    void getByIdShouldReturn404WhenNotExists() {
        when(service.findById(99L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, liveEnvironmentController.getById(99L).getStatusCode());
    }

    @Test
    void createShouldReturnSavedEnv() {
        when(service.save(any(LiveEnvironment.class))).thenReturn(testEnv);

        LiveEnvironment result = liveEnvironmentController.create(testEnv);

        assertNotNull(result);
        assertEquals(testEnv, result);
        verify(service).save(testEnv);
    }

    @Test
    void updateShouldReturnUpdatedWhenExists() {
        when(service.findById(1L)).thenReturn(Optional.of(testEnv));
        when(service.save(any(LiveEnvironment.class))).thenReturn(testEnv);

        ResponseEntity<LiveEnvironment> response = liveEnvironmentController.update(1L, testEnv);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testEnv, response.getBody());
    }

    @Test
    void updateShouldReturn404WhenNotExists() {
        when(service.findById(99L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, liveEnvironmentController.update(99L, testEnv).getStatusCode());
        verify(service, never()).save(any());
    }

    @Test
    void deleteShouldReturnNoContent() {
        when(service.findById(1L)).thenReturn(Optional.of(testEnv));

        ResponseEntity<Void> response = liveEnvironmentController.delete(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(service).deleteById(1L);
    }

    @Test
    void deleteShouldReturn404WhenNotExists() {
        when(service.findById(99L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, liveEnvironmentController.delete(99L).getStatusCode());
        verify(service, never()).deleteById(any());
    }

    @Test
    void getMaxVncPortShouldReturnMaxPort() {
        when(service.getMaxVncPort()).thenReturn(5901);

        Integer result = liveEnvironmentController.getMaxVncPort();

        assertEquals(5901, result);
        verify(service).getMaxVncPort();
    }

    @Test
    void getMaxVncPortShouldReturnNullWhenNoEntries() {
        when(service.getMaxVncPort()).thenReturn(null);

        assertNull(liveEnvironmentController.getMaxVncPort());
    }
}

