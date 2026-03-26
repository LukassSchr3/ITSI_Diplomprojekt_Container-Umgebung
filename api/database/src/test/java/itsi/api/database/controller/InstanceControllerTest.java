package itsi.api.database.controller;

import itsi.api.database.entity.Image;
import itsi.api.database.entity.Instance;
import itsi.api.database.entity.User;
import itsi.api.database.service.InstanceService;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstanceControllerTest {

    @Mock
    private InstanceService instanceService;

    @InjectMocks
    private InstanceController instanceController;

    private Instance testInstance;

    @BeforeEach
    void setUp() {
        Image image = new Image(1, "ubuntu-test", "ubuntu:22.04");
        User user = new User();
        user.setId(1);
        user.setName("testuser");

        testInstance = new Instance();
        testInstance.setId(1);
        testInstance.setContainerId("abc123");
        testInstance.setName("test-container");
        testInstance.setImage(image);
        testInstance.setUser(user);
        testInstance.setStatus("running");
    }

    @Test
    void getAllInstances_shouldReturnList() {
        when(instanceService.findAll()).thenReturn(Arrays.asList(testInstance));

        ResponseEntity<List<Instance>> response = instanceController.getAllInstances();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllInstances_shouldReturnEmptyList() {
        when(instanceService.findAll()).thenReturn(Collections.emptyList());

        assertTrue(instanceController.getAllInstances().getBody().isEmpty());
    }

    @Test
    void getInstanceById_shouldReturnWhenExists() {
        when(instanceService.findById(1)).thenReturn(Optional.of(testInstance));

        ResponseEntity<Instance> response = instanceController.getInstanceById(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testInstance, response.getBody());
    }

    @Test
    void getInstanceById_shouldReturn404WhenNotExists() {
        when(instanceService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, instanceController.getInstanceById(99).getStatusCode());
    }

    @Test
    void getInstanceByContainerId_shouldReturnWhenExists() {
        when(instanceService.findByContainerId("abc123")).thenReturn(Optional.of(testInstance));

        ResponseEntity<Instance> response = instanceController.getInstanceByContainerId("abc123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(testInstance, response.getBody());
    }

    @Test
    void getInstanceByContainerId_shouldReturn404WhenNotExists() {
        when(instanceService.findByContainerId("unknown")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, instanceController.getInstanceByContainerId("unknown").getStatusCode());
    }

    @Test
    void getInstanceByName_shouldReturnWhenExists() {
        when(instanceService.findByName("test-container")).thenReturn(Optional.of(testInstance));

        ResponseEntity<Instance> response = instanceController.getInstanceByName("test-container");

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void getInstanceByName_shouldReturn404WhenNotExists() {
        when(instanceService.findByName("unknown")).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, instanceController.getInstanceByName("unknown").getStatusCode());
    }

    @Test
    void getInstancesByUserId_shouldReturnList() {
        when(instanceService.findByUserId(1)).thenReturn(Arrays.asList(testInstance));

        ResponseEntity<List<Instance>> response = instanceController.getInstancesByUserId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getInstancesByImageId_shouldReturnList() {
        when(instanceService.findByImageId(1)).thenReturn(Arrays.asList(testInstance));

        ResponseEntity<List<Instance>> response = instanceController.getInstancesByImageId(1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getInstancesByStatus_shouldReturnList() {
        when(instanceService.findByStatus("running")).thenReturn(Arrays.asList(testInstance));

        ResponseEntity<List<Instance>> response = instanceController.getInstancesByStatus("running");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createInstance_shouldReturnCreated() {
        when(instanceService.save(any(Instance.class))).thenReturn(testInstance);

        ResponseEntity<Instance> response = instanceController.createInstance(testInstance);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(testInstance, response.getBody());
    }

    @Test
    void updateInstance_shouldReturnUpdated() {
        when(instanceService.findById(1)).thenReturn(Optional.of(testInstance));
        when(instanceService.save(any(Instance.class))).thenReturn(testInstance);

        ResponseEntity<Instance> response = instanceController.updateInstance(1, testInstance);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void updateInstance_shouldReturn404WhenNotExists() {
        when(instanceService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, instanceController.updateInstance(99, testInstance).getStatusCode());
    }

    @Test
    void deleteInstance_shouldReturnNoContent() {
        when(instanceService.findById(1)).thenReturn(Optional.of(testInstance));

        ResponseEntity<Void> response = instanceController.deleteInstance(1);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(instanceService).deleteById(1);
    }

    @Test
    void deleteInstance_shouldReturn404WhenNotExists() {
        when(instanceService.findById(99)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, instanceController.deleteInstance(99).getStatusCode());
        verify(instanceService, never()).deleteById(any());
    }

    @Test
    void getMaxContainerId_shouldReturnOkWhenExists() {
        when(instanceService.findMaxContainerId()).thenReturn(Optional.of("abc999"));

        ResponseEntity<String> response = instanceController.getMaxContainerId();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("abc999", response.getBody());
    }

    @Test
    void getMaxContainerId_shouldReturnNoContentWhenEmpty() {
        when(instanceService.findMaxContainerId()).thenReturn(Optional.empty());

        ResponseEntity<String> response = instanceController.getMaxContainerId();

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}

