package itsi.api.steuerung.service;

import itsi.api.steuerung.dto.ImageDTO;
import itsi.api.steuerung.dto.InstanceDTO;
import itsi.api.steuerung.dto.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class DatabaseServiceTest {

    @Mock private WebClient databaseWebClient;

    // GET chain
    @Mock private WebClient.RequestHeadersUriSpec getUriSpec;
    @Mock private WebClient.RequestHeadersSpec getHeadersSpec;
    @Mock private WebClient.ResponseSpec getResponseSpec;

    // POST chain
    @Mock private WebClient.RequestBodyUriSpec postUriSpec;
    @Mock private WebClient.RequestBodySpec postBodySpec;
    @Mock private WebClient.RequestHeadersSpec postHeadersSpec;
    @Mock private WebClient.ResponseSpec postResponseSpec;

    // PUT chain
    @Mock private WebClient.RequestBodyUriSpec putUriSpec;
    @Mock private WebClient.RequestBodySpec putBodySpec;
    @Mock private WebClient.RequestHeadersSpec putHeadersSpec;
    @Mock private WebClient.ResponseSpec putResponseSpec;

    // DELETE chain
    @Mock private WebClient.RequestHeadersUriSpec deleteUriSpec;
    @Mock private WebClient.RequestHeadersSpec deleteHeadersSpec;
    @Mock private WebClient.ResponseSpec deleteResponseSpec;

    private DatabaseService databaseService;

    @BeforeEach
    void setUp() {
        databaseService = new DatabaseService(databaseWebClient);
    }

    // ===================== Helper: mock GET =====================

    private void mockGet(Object returnValue) {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(returnValue));
    }

    private void mockGetWithUriString(Object returnValue) {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(returnValue));
    }

    private void mockGetError(Exception ex) {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.error(ex));
    }

    // ===================== getInstanceById =====================

    @Test
    void getInstanceByIdReturnsInstance() {
        InstanceDTO inst = new InstanceDTO(5, "cont_5", "test", null, null, "running");
        mockGet(inst);
        InstanceDTO result = databaseService.getInstanceById(5);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(5);
    }

    // ===================== getInstanceByContainerId =====================

    @Test
    void getInstanceByContainerIdReturnsInstance() {
        InstanceDTO inst = new InstanceDTO(3, "cont_3", "test", null, null, "stopped");
        mockGet(inst);
        InstanceDTO result = databaseService.getInstanceByContainerId("cont_3");
        assertThat(result).isNotNull();
        assertThat(result.getContainerId()).isEqualTo("cont_3");
    }

    // ===================== getUserById =====================

    @Test
    void getUserByIdReturnsUser() {
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        mockGet(user);
        UserDTO result = databaseService.getUserById(1);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Max");
    }

    // ===================== getImageById =====================

    @Test
    void getImageByIdReturnsImage() {
        ImageDTO img = new ImageDTO(2, "ubuntu", "ubuntu:22.04");
        mockGet(img);
        ImageDTO result = databaseService.getImageById(2);
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("ubuntu");
    }

    // ===================== getMaxContainerId =====================

    @Test
    void getMaxContainerIdReturnsMaxId() {
        mockGet("cont_5");
        String result = databaseService.getMaxContainerId();
        assertThat(result).isEqualTo("cont_5");
    }

    @Test
    void getMaxContainerIdReturnsNullWhenEmpty() {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(String.class))).thenReturn(Mono.empty());
        String result = databaseService.getMaxContainerId();
        assertThat(result).isNull();
    }

    // ===================== createInstance =====================

    @Test
    void createInstanceReturnsCreatedInstance() {
        InstanceDTO inst = new InstanceDTO(10, "", "ubuntu_Max", null, null, "created");
        when(databaseWebClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(postResponseSpec);
        when(postResponseSpec.bodyToMono(eq(InstanceDTO.class))).thenReturn(Mono.just(inst));

        InstanceDTO result = databaseService.createInstance(inst);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
    }

    // ===================== updateInstance =====================

    @Test
    void updateInstanceReturnsUpdatedInstance() {
        InstanceDTO inst = new InstanceDTO(10, "cont_1", "ubuntu_Max", null, null, "running");
        when(databaseWebClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(anyString(), any(Object[].class))).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.bodyToMono(eq(InstanceDTO.class))).thenReturn(Mono.just(inst));

        InstanceDTO result = databaseService.updateInstance(10, inst);
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo("running");
    }

    // ===================== findUserByEmail =====================

    @Test
    void findUserByEmailReturnsUserInOptional() {
        UserDTO user = new UserDTO(1, "Max", "max@test.at", "pw", "5A", "SCHUELER", null, null);
        mockGet(user);
        Optional<UserDTO> result = databaseService.findUserByEmail("max@test.at");
        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("max@test.at");
    }

    @Test
    void findUserByEmailReturnsEmptyOptionalOnException() {
        mockGetError(new RuntimeException("not found"));
        Optional<UserDTO> result = databaseService.findUserByEmail("nobody@test.at");
        assertThat(result).isEmpty();
    }

    // ===================== getLiveEnvironmentByUserId =====================

    @Test
    void getLiveEnvironmentByUserIdReturnsMap() {
        Map<String, Object> liveEnv = Map.of("id", 1, "status", "running");
        mockGet(liveEnv);
        Map<String, Object> result = databaseService.getLiveEnvironmentByUserId(1);
        assertThat(result).isNotNull();
        assertThat(result.get("status")).isEqualTo("running");
    }

    @Test
    void getLiveEnvironmentByUserIdReturnsNullOnException() {
        mockGetError(new RuntimeException("not found"));
        Map<String, Object> result = databaseService.getLiveEnvironmentByUserId(99);
        assertThat(result).isNull();
    }

    // ===================== createImage =====================

    @Test
    void createImageReturnsCreatedImage() {
        Map<String, Object> img = Map.of("id", 5, "name", "alpine");
        when(databaseWebClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(postResponseSpec);
        when(postResponseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(img));

        Map<String, Object> result = databaseService.createImage(img);
        assertThat(result).isNotNull();
        assertThat(result.get("name")).isEqualTo("alpine");
    }

    // ===================== deleteInstance =====================

    @Test
    void deleteInstanceExecutesWithoutException() {
        when(databaseWebClient.delete()).thenReturn(deleteUriSpec);
        when(deleteUriSpec.uri(anyString(), any(Object[].class))).thenReturn(deleteHeadersSpec);
        when(deleteHeadersSpec.retrieve()).thenReturn(deleteResponseSpec);
        when(deleteResponseSpec.bodyToMono(eq(Void.class))).thenReturn(Mono.empty());

        databaseService.deleteInstance(5);
        verify(databaseWebClient).delete();
    }

    // ===================== deleteImage =====================

    @Test
    void deleteImageExecutesWithoutException() {
        when(databaseWebClient.delete()).thenReturn(deleteUriSpec);
        when(deleteUriSpec.uri(anyString(), any(Object[].class))).thenReturn(deleteHeadersSpec);
        when(deleteHeadersSpec.retrieve()).thenReturn(deleteResponseSpec);
        when(deleteResponseSpec.bodyToMono(eq(Void.class))).thenReturn(Mono.empty());

        databaseService.deleteImage(3);
        verify(databaseWebClient).delete();
    }

    // ===================== getInstancesByUserAndImage =====================

    @Test
    void getInstancesByUserAndImageFiltersCorrectly() {
        ImageDTO img1 = new ImageDTO(2, "ubuntu", "ubuntu:22");
        ImageDTO img2 = new ImageDTO(3, "alpine", "alpine:3");
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO i1 = new InstanceDTO(10, "c1", "n1", img1, user, "running");
        InstanceDTO i2 = new InstanceDTO(11, "c2", "n2", img2, user, "stopped");
        InstanceDTO[] all = {i1, i2};

        mockGet(all);

        InstanceDTO[] result = databaseService.getInstancesByUserAndImage(1, 2);
        assertThat(result).hasSize(1);
        assertThat(result[0].getId()).isEqualTo(10);
    }

    @Test
    void getInstancesByUserAndImageReturnsEmptyArrayWhenNullResponse() {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(InstanceDTO[].class))).thenReturn(Mono.empty());

        InstanceDTO[] result = databaseService.getInstancesByUserAndImage(1, 2);
        assertThat(result).isEmpty();
    }

    @Test
    void getInstancesByUserAndImageNoMatchReturnsEmptyArray() {
        ImageDTO img = new ImageDTO(99, "other", "other:1");
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO i1 = new InstanceDTO(10, "c1", "n1", img, user, "running");
        InstanceDTO[] all = {i1};

        mockGet(all);

        InstanceDTO[] result = databaseService.getInstancesByUserAndImage(1, 2);
        assertThat(result).isEmpty();
    }

    // ===================== findOrCreateInstance =====================

    @Test
    void findOrCreateInstanceReturnsExistingInstance() {
        ImageDTO img = new ImageDTO(2, "ubuntu", "ubuntu:22");
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO existing = new InstanceDTO(10, "cont_1", "ubuntu_Max", img, user, "running");
        InstanceDTO[] existingArr = {existing};

        mockGet(existingArr);

        InstanceDTO result = databaseService.findOrCreateInstance(1, 2);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10);
    }

    @Test
    void findOrCreateInstanceCreatesNewInstanceWhenNoneExists() {
        // First call (getInstancesByUserAndImage) returns empty array
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);

        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        ImageDTO img = new ImageDTO(2, "ubuntu", "ubuntu:22");
        InstanceDTO created = new InstanceDTO(20, "", "Max_ubuntu", img, user, "created");

        // Return empty array on first call, then user, then image
        when(getResponseSpec.bodyToMono(eq(InstanceDTO[].class)))
                .thenReturn(Mono.just(new InstanceDTO[0]));
        when(getResponseSpec.bodyToMono(eq(UserDTO.class)))
                .thenReturn(Mono.just(user));
        when(getResponseSpec.bodyToMono(eq(ImageDTO.class)))
                .thenReturn(Mono.just(img));

        when(databaseWebClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(postBodySpec);
        when(postBodySpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(postResponseSpec);
        when(postResponseSpec.bodyToMono(eq(InstanceDTO.class))).thenReturn(Mono.just(created));

        InstanceDTO result = databaseService.findOrCreateInstance(1, 2);
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(20);
    }

    // ===================== updateInstanceStatus =====================

    @Test
    void updateInstanceStatusCompletesSuccessfully() {
        InstanceDTO inst = new InstanceDTO(5, "cont_5", "test", null, null, "running");

        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(InstanceDTO.class))).thenReturn(Mono.just(inst));

        when(databaseWebClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(anyString(), any(Object[].class))).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.bodyToMono(eq(InstanceDTO.class))).thenReturn(Mono.just(inst));

        // Should not throw
        databaseService.updateInstanceStatus(5, "stopped").block();
        verify(databaseWebClient, atLeastOnce()).put();
    }

    @Test
    void updateInstanceStatusOnErrorResumesGracefully() {
        when(databaseWebClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString(), any(Object[].class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(getResponseSpec);
        when(getResponseSpec.bodyToMono(eq(InstanceDTO.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        // Should complete without error due to onErrorResume
        databaseService.updateInstanceStatus(5, "stopped").block();
    }

    // ===================== updateLiveEnvironmentStatus =====================

    @Test
    void updateLiveEnvironmentStatusCompletesSuccessfully() {
        Map<String, Object> result = Map.of("id", 1, "status", "stopped");

        when(databaseWebClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(anyString(), any(Object[].class))).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.bodyToMono(any(Class.class))).thenReturn(Mono.just(result));

        databaseService.updateLiveEnvironmentStatus(1, "stopped").block();
        verify(databaseWebClient, atLeastOnce()).put();
    }

    @Test
    void updateLiveEnvironmentStatusOnErrorResumesGracefully() {
        when(databaseWebClient.put()).thenReturn(putUriSpec);
        when(putUriSpec.uri(anyString(), any(Object[].class))).thenReturn(putBodySpec);
        when(putBodySpec.bodyValue(any())).thenReturn(putHeadersSpec);
        when(putHeadersSpec.retrieve()).thenReturn(putResponseSpec);
        when(putResponseSpec.bodyToMono(any(Class.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        // Should complete without error due to onErrorResume
        databaseService.updateLiveEnvironmentStatus(1, "stopped").block();
    }
}

