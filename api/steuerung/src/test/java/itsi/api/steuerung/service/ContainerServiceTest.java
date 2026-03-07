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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void startContainer_existingInstanceWithContainerIdReturnsSuccess() {
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
    void startContainer_instanceWithNoContainerIdCallsCreateAndStart() {
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
    void startContainer_nullContainerIdCallsCreateAndStart() {
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
    void startContainer_instanceMissingImageRefReturnsFailure() {
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
    void startContainer_serviceThrowsReturnsFailure() {
        when(databaseService.findOrCreateInstance(1, 2))
                .thenThrow(new RuntimeException("DB error"));

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("Error");
    }

    // ===================== stopContainer =====================

    @Test
    void stopContainer_successReturnsStoppedStatus() {
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
    void stopContainer_noInstanceFoundReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[0]);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void stopContainer_nullInstancesArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    @Test
    void stopContainer_instanceWithoutImageRefStillSendsRequest() {
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
    void stopContainer_serviceThrowsReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenThrow(new RuntimeException("network error"));

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    // ===================== resetContainer =====================

    @Test
    void resetContainer_successReturnsRunningStatus() {
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
    void resetContainer_noInstanceFoundReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenReturn(new InstanceDTO[0]);

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void resetContainer_missingImageRefReturnsFailure() {
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
    void resetContainer_serviceThrowsReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2))
                .thenThrow(new RuntimeException("timeout"));

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
    }

    // ===================== createAndStartContainerIfMissing =====================

    @Test
    void createAndStartContainerIfMissing_alreadyHasContainerIdReturnsInstance() {
        InstanceDTO instance = buildInstance("cont_7");
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(instance);

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNotNull();
        assertThat(result.getContainerId()).isEqualTo("cont_7");
        verify(backendWebClient, never()).post();
    }

    @Test
    void createAndStartContainerIfMissing_noContainerIdCreatesNewOne() {
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
    void createAndStartContainerIfMissing_nullMaxContainerIdStartsFromOne() {
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
    void createAndStartContainerIfMissing_unparsableMaxIdDefaultsToOne() {
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
    void createAndStartContainerIfMissing_nullInstanceFromDbReturnsNull() {
        when(databaseService.findOrCreateInstance(1, 2)).thenReturn(null);

        InstanceDTO result = containerService.createAndStartContainerIfMissing(1, 2);

        assertThat(result).isNull();
    }

    @Test
    void createAndStartContainerIfMissing_noImageRefReturnsNull() {
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
    void startContainer_emptyContainerIdCallsCreateAndStart() {
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
    void startContainer_failedCreateAndStartReturnsFailure() {
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
    void startContainer_requestWithNullUserIdStillHandled() {
        when(databaseService.findOrCreateInstance(null, 2))
                .thenThrow(new RuntimeException("null userId"));

        ContainerOperationResponse resp = containerService.startContainer(
                new ContainerOperationRequest(null, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("Error");
    }

    // ===================== Additional stopContainer edge cases =====================

    @Test
    void stopContainer_nullArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.stopContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void stopContainer_multipleInstancesUsesFirst() {
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
    void resetContainer_nullInstancesArrayReturnsFailure() {
        when(databaseService.getInstancesByUserAndImage(1, 2)).thenReturn(null);

        ContainerOperationResponse resp = containerService.resetContainer(
                new ContainerOperationRequest(1, 2));

        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).contains("No instance found");
    }

    @Test
    void resetContainer_multipleInstancesUsesFirst() {
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
    void resetContainer_noImageNullRefStillSendsRequest() {
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
    void createAndStartContainerIfMissing_noUserNameFallsBackToUser() {
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
    void createAndStartContainerIfMissing_noImageNameFallsBackToImg() {
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
    void createAndStartContainerIfMissing_maxContainerIdIsZeroStartsAtOne() {
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

