package itsi.api.steuerung.controller;

import itsi.api.steuerung.dto.ContainerOperationRequest;
import itsi.api.steuerung.dto.ContainerOperationResponse;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.service.ContainerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerControllerTest {

    @Mock
    private ContainerService containerService;

    @InjectMocks
    private ContainerController containerController;

    private ContainerOperationRequest request;
    private InstanceDTO instance;

    @BeforeEach
    void setUp() {
        request = new ContainerOperationRequest(1, 2);
        instance = new InstanceDTO(10, "cont_1", "img_user", null, null, "running");
    }

    // --- startContainer ---

    @Test
    void startContainerSuccessReturns200() {
        ContainerOperationResponse resp = new ContainerOperationResponse(
                true, "Container started successfully", "cont_1", "running", instance);
        when(containerService.startContainer(request)).thenReturn(resp);

        ResponseEntity<ContainerOperationResponse> response = containerController.startContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().isSuccess()).isTrue();
        assertThat(response.getBody().getContainerId()).isEqualTo("cont_1");
    }

    @Test
    void startContainerFailureReturnsBadRequest() {
        ContainerOperationResponse resp = new ContainerOperationResponse(
                false, "Failed", null, null, null);
        when(containerService.startContainer(request)).thenReturn(resp);

        ResponseEntity<ContainerOperationResponse> response = containerController.startContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().isSuccess()).isFalse();
    }

    @Test
    void startContainerDelegatesToService() {
        when(containerService.startContainer(request))
                .thenReturn(new ContainerOperationResponse(true, "ok", "c1", "running", null));

        containerController.startContainer(request);

        verify(containerService, times(1)).startContainer(request);
    }

    @Test
    void startContainerSuccessResponseContainsInstance() {
        ContainerOperationResponse resp = new ContainerOperationResponse(
                true, "ok", "cont_1", "running", instance);
        when(containerService.startContainer(request)).thenReturn(resp);

        ResponseEntity<ContainerOperationResponse> response = containerController.startContainer(request);

        assertThat(response.getBody().getInstance()).isEqualTo(instance);
        assertThat(response.getBody().getStatus()).isEqualTo("running");
    }

    // --- stopContainer ---

    @Test
    void stopContainerSuccessReturns200() {
        ContainerOperationResponse resp = new ContainerOperationResponse(
                true, "Container stopped successfully", "cont_1", "stopped", instance);
        when(containerService.stopContainer(request)).thenReturn(resp);

        ResponseEntity<ContainerOperationResponse> response = containerController.stopContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo("stopped");
    }

    @Test
    void stopContainerFailureReturnsBadRequest() {
        when(containerService.stopContainer(request))
                .thenReturn(new ContainerOperationResponse(false, "No instance found", null, null, null));

        ResponseEntity<ContainerOperationResponse> response = containerController.stopContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void stopContainerDelegatesToService() {
        when(containerService.stopContainer(request))
                .thenReturn(new ContainerOperationResponse(true, "ok", "c1", "stopped", null));

        containerController.stopContainer(request);

        verify(containerService, times(1)).stopContainer(request);
    }

    // --- resetContainer ---

    @Test
    void resetContainerSuccessReturns200() {
        ContainerOperationResponse resp = new ContainerOperationResponse(
                true, "Container reset successfully", "cont_1", "running", instance);
        when(containerService.resetContainer(request)).thenReturn(resp);

        ResponseEntity<ContainerOperationResponse> response = containerController.resetContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("reset");
    }

    @Test
    void resetContainerFailureReturnsBadRequest() {
        when(containerService.resetContainer(request))
                .thenReturn(new ContainerOperationResponse(false, "Error", null, null, null));

        ResponseEntity<ContainerOperationResponse> response = containerController.resetContainer(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetContainerDelegatesToService() {
        when(containerService.resetContainer(request))
                .thenReturn(new ContainerOperationResponse(true, "ok", "c1", "running", null));

        containerController.resetContainer(request);

        verify(containerService, times(1)).resetContainer(request);
    }
}

