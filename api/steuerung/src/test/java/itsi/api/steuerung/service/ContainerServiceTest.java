package itsi.api.steuerung.service;

import itsi.api.steuerung.dto.ContainerOperationRequest;
import itsi.api.steuerung.dto.ContainerOperationResponse;
import itsi.api.steuerung.dto.ImageDTO;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContainerServiceTest {

    @Mock
    private WebClient backendWebClient;

    @Mock
    private DatabaseService databaseService;

    @InjectMocks
    private ContainerService containerService;

    // WebClient chain mocks
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestBodySpec requestBodySpec;
    @Mock private WebClient.ResponseSpec responseSpec;
    @SuppressWarnings("rawtypes")
    @Mock private WebClient.RequestHeadersSpec requestHeadersSpec;

    private InstanceDTO buildInstance(String containerId) {
        ImageDTO image = new ImageDTO(2, "ubuntu", "ubuntu:22.04");
        UserDTO user = new UserDTO(1, "MaxMuster", "max@test.at", null, "5AHIT", "SCHUELER", null, null);
        InstanceDTO inst = new InstanceDTO(10, containerId, "ubuntu_MaxMuster", image, user, "running");
        return inst;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void mockBackendPost() {
        when(backendWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(eq(java.util.Map.class)))
                .thenReturn(Mono.just(java.util.Map.of("status", "ok")));
    }

    // ===================== startContainer =====================

    @Test
    void startContainerExistingInstanceWithContainerIdReturnsSuccess() {
        InstanceDTO instance = buildInstance("cont_5");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instance);
        when(databaseService.updateInstance(eq(10), any())).thenReturn(instance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContainerId()).isEqualTo("cont_5");
        assertThat(resp.getStatus()).isEqualTo("running");
    }

    @Test
    void startContainerInstanceWithNoContainerIdCallsCreateAndStart() {
        InstanceDTO instanceNoId = buildInstance("");
        InstanceDTO createdInstance = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        // createAndStartContainerIfMissing path
        when(databaseService.getMaxContainerId()).thenReturn("cont_0");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(createdInstance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    void startContainerNullContainerIdCallsCreateAndStart() {
        InstanceDTO instanceNoId = buildInstance(null);
        InstanceDTO createdInstance = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn(null);
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(createdInstance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    void startContainerInstanceMissingImageRefReturnsFailure() {
        ImageDTO imageNoRef = new ImageDTO(2, "ubuntu", null);
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instance = new InstanceDTO(10, "cont_5", "ubuntu_Max", imageNoRef, user, "stopped");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instance);

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("image reference");
    }

    @Test
    void startContainerServiceThrowsReturnsFailure() {
        when(databaseService.findOrCreateInstance(1, 2))
                .thenThrow(new RuntimeException("DB error"));

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("Error");
    }

    // ===================== stopContainer =====================

    @Test
    void stopContainerSuccessReturnsStoppedStatus() {
        InstanceDTO instance = buildInstance("cont_5");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{instance});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(instance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("stopped");
    }

    @Test
    void stopContainerNoInstanceFoundReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[0]);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void stopContainerNullInstancesArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    @Test
    void stopContainerInstanceWithoutImageRefStillSendsRequest() {
        ImageDTO imageNoRef = new ImageDTO(2, "ubuntu", null);
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instance = new InstanceDTO(10, "cont_5", "ubuntu_Max", imageNoRef, user, "running");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{instance});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(instance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
    }

    @Test
    void stopContainerServiceThrowsReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenThrow(new RuntimeException("network error"));

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    // ===================== resetContainer =====================

    @Test
    void resetContainerSuccessReturnsRunningStatus() {
        InstanceDTO instance = buildInstance("cont_5");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{instance});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(instance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("running");
    }

    @Test
    void resetContainerNoInstanceFoundReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[0]);

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void resetContainerMissingImageRefReturnsFailure() {
        ImageDTO imageNoRef = new ImageDTO(2, "ubuntu", null);
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instance = new InstanceDTO(10, "cont_5", "ubuntu_Max", imageNoRef, user, "running");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{instance});

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("image reference");
    }

    @Test
    void resetContainerServiceThrowsReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenThrow(new RuntimeException("timeout"));

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    // ===================== createAndStartContainerIfMissing =====================

    @Test
    void createAndStartContainerIfMissingAlreadyHasContainerIdReturnsInstance() {
        InstanceDTO instance = buildInstance("cont_7");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instance);

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getContainerId()).isEqualTo("cont_7");
        verify(backendWebClient, never()).post();
    }

    @Test
    void createAndStartContainerIfMissingNoContainerIdCreatesNewOne() {
        InstanceDTO instanceNoId = buildInstance("");
        InstanceDTO updated = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("cont_0");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getContainerId()).isEqualTo("cont_1");
    }

    @Test
    void createAndStartContainerIfMissingNullMaxContainerIdStartsFromOne() {
        InstanceDTO instanceNoId = buildInstance(null);
        InstanceDTO updated = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn(null);
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
    }

    @Test
    void createAndStartContainerIfMissingUnparsableMaxIdDefaultsToOne() {
        InstanceDTO instanceNoId = buildInstance("");
        InstanceDTO updated = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("invalid");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
    }

    @Test
    void createAndStartContainerIfMissingNullInstanceFromDbReturnsNull() {
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(null);

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNull();
    }

    @Test
    void createAndStartContainerIfMissingNoImageRefReturnsNull() {
        ImageDTO imageNoRef = new ImageDTO(2, "ubuntu", null);
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instance = new InstanceDTO(10, "", "ubuntu_Max", imageNoRef, user, "stopped");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instance);
        when(databaseService.getMaxContainerId()).thenReturn("cont_0");

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNull();
    }

    // ===================== Additional startContainer edge cases =====================

    @Test
    void startContainerEmptyContainerIdCallsCreateAndStart() {
        InstanceDTO instanceNoId = buildInstance("");
        InstanceDTO createdInstance = buildInstance("cont_3");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("cont_2");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(createdInstance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContainerId()).isEqualTo("cont_3");
    }

    @Test
    void startContainerFailedCreateAndStartReturnsFailure() {
        InstanceDTO instanceNoId = buildInstance(null);
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        // createAndStartContainerIfMissing will return null due to missing imageRef
        ImageDTO imageNoRef = new ImageDTO(2, "ubuntu", null);
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO noRefInstance = new InstanceDTO(10, null, "ubuntu_Max", imageNoRef, user, "stopped");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(noRefInstance);
        when(databaseService.getMaxContainerId()).thenReturn("cont_0");

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    @Test
    void startContainerRequestWithNullUserIdStillHandled() {
        when(databaseService.findOrCreateInstance(null, 2))
                .thenThrow(new RuntimeException("null userId"));

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(null, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("Error");
    }

    // ===================== Additional stopContainer edge cases =====================

    @Test
    void stopContainerNullArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void stopContainerMultipleInstancesUsesFirst() {
        InstanceDTO inst1 = buildInstance("cont_5");
        InstanceDTO inst2 = buildInstance("cont_6");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{inst1, inst2});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(inst1);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContainerId()).isEqualTo("cont_5");
    }

    // ===================== Additional resetContainer edge cases =====================

    @Test
    void resetContainerNullInstancesArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void resetContainerMultipleInstancesUsesFirst() {
        InstanceDTO inst1 = buildInstance("cont_7");
        InstanceDTO inst2 = buildInstance("cont_8");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{inst1, inst2});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(inst1);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getContainerId()).isEqualTo("cont_7");
    }

    @Test
    void resetContainerNoImageNullRefStillSendsRequest() {
        InstanceDTO instance = buildInstance("cont_5");
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[]{instance});
        when(databaseService.updateInstance(eq(10), any())).thenReturn(instance);
        mockBackendPost();

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getStatus()).isEqualTo("running");
    }

    // ===================== createAndStartContainerIfMissing – more edge cases =====================

    @Test
    void createAndStartContainerIfMissingNoUserNameFallsBackToUser() {
        ImageDTO img = new ImageDTO(2, "ubuntu", "ubuntu:22");
        UserDTO userNoName = new UserDTO(1, null, "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instanceNoId = new InstanceDTO(10, "", "ubuntu_", img, userNoName, "stopped");
        InstanceDTO updated = buildInstance("cont_2");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("cont_1");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
    }

    @Test
    void createAndStartContainerIfMissingNoImageNameFallsBackToImg() {
        ImageDTO imgNoName = new ImageDTO(2, null, "ubuntu:22");
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO instanceNoId = new InstanceDTO(10, "", "_Max", imgNoName, user, "stopped");
        InstanceDTO updated = buildInstance("cont_2");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("cont_1");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
    }

    @Test
    void createAndStartContainerIfMissingMaxContainerIdIsZeroStartsAtOne() {
        InstanceDTO instanceNoId = buildInstance("");
        InstanceDTO updated = buildInstance("cont_1");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instanceNoId);
        when(databaseService.getMaxContainerId()).thenReturn("cont_0");
        when(databaseService.updateInstance(anyInt(), any())).thenReturn(updated);
        mockBackendPost();

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);
        assertThat(result).isNotNull();
        assertThat(result.getContainerId()).isEqualTo("cont_1");
    }
}

