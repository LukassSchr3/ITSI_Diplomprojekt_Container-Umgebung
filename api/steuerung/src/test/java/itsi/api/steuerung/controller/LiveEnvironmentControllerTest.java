package itsi.api.steuerung.controller;

import itsi.api.steuerung.websocket.LiveEnvironmentWebSocketHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class LiveEnvironmentControllerTest {

    @Mock private WebClient databaseWebClient;
    @Mock private WebClient backendWebClient;
    @Mock private LiveEnvironmentWebSocketHandler wsHandler;

    // DB GET chain
    @Mock private WebClient.RequestHeadersUriSpec dbGetUriSpec;
    @Mock private WebClient.RequestHeadersSpec dbGetHeadersSpec;
    @Mock private WebClient.ResponseSpec dbGetResponseSpec;

    // DB POST chain
    @Mock private WebClient.RequestBodyUriSpec dbPostUriSpec;
    @Mock private WebClient.RequestBodySpec dbPostBodySpec;
    @Mock private WebClient.RequestHeadersSpec dbPostHeadersSpec;
    @Mock private WebClient.ResponseSpec dbPostResponseSpec;

    // DB PUT chain
    @Mock private WebClient.RequestBodyUriSpec dbPutUriSpec;
    @Mock private WebClient.RequestBodySpec dbPutBodySpec;
    @Mock private WebClient.RequestHeadersSpec dbPutHeadersSpec;
    @Mock private WebClient.ResponseSpec dbPutResponseSpec;

    // Backend POST chain
    @Mock private WebClient.RequestBodyUriSpec bePostUriSpec;
    @Mock private WebClient.RequestBodySpec bePostBodySpec;
    @Mock private WebClient.RequestHeadersSpec bePostHeadersSpec;
    @Mock private WebClient.ResponseSpec bePostResponseSpec;

    private LiveEnvironmentController controller;

    @BeforeEach
    void setUp() {
        controller = new LiveEnvironmentController(databaseWebClient, backendWebClient, wsHandler);
    }

    // ===================== Helpers =====================

    private void mockDbPost(Map<String, Object> returnValue) {
        when(databaseWebClient.post()).thenReturn(dbPostUriSpec);
        when(dbPostUriSpec.uri(anyString())).thenReturn(dbPostBodySpec);
        when(dbPostBodySpec.bodyValue(any())).thenReturn(dbPostHeadersSpec);
        when(dbPostHeadersSpec.retrieve()).thenReturn(dbPostResponseSpec);
        when(dbPostResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(returnValue));
    }

    private void mockDbPut(Map<String, Object> returnValue) {
        when(databaseWebClient.put()).thenReturn(dbPutUriSpec);
        when(dbPutUriSpec.uri(anyString())).thenReturn(dbPutBodySpec);
        when(dbPutBodySpec.bodyValue(any())).thenReturn(dbPutHeadersSpec);
        when(dbPutHeadersSpec.retrieve()).thenReturn(dbPutResponseSpec);
        when(dbPutResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(returnValue));
    }

    private void mockBackendPost(Map<String, Object> returnValue) {
        when(backendWebClient.post()).thenReturn(bePostUriSpec);
        when(bePostUriSpec.uri(anyString())).thenReturn(bePostBodySpec);
        when(bePostBodySpec.bodyValue(any())).thenReturn(bePostHeadersSpec);
        when(bePostHeadersSpec.retrieve()).thenReturn(bePostResponseSpec);
        when(bePostResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(returnValue));
    }

    // ===================== createLiveEnvironment =====================

    @Test
    void createLiveEnvironment_withUserId_setsVncPortBasedOnUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 5);
        input.put("vncPassword", "secret");

        Map<String, Object> created = new HashMap<>(input);
        created.put("id", 1);
        created.put("vncPort", 5905);
        mockDbPost(created);

        ResponseEntity<?> response = controller.createLiveEnvironment(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(databaseWebClient).post();
    }

    @Test
    void createLiveEnvironment_vncPortEqualsUserId() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 10);
        input.put("vncPassword", "pw");

        Map<String, Object> created = Map.of("id", 2, "vncPort", 5910);
        mockDbPost(created);

        controller.createLiveEnvironment(input);

        // vncPort muss 5900 + userId = 5910 sein
        assertThat(input.get("vncPort")).isEqualTo(5910);
    }

    @Test
    void createLiveEnvironment_withoutUserId_returnsBadRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("vncPassword", "secret");

        ResponseEntity<?> response = controller.createLiveEnvironment(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createLiveEnvironment_withoutVncPassword_returnsBadRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 3);

        ResponseEntity<?> response = controller.createLiveEnvironment(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createLiveEnvironment_withEmptyVncPassword_returnsBadRequest() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 3);
        input.put("vncPassword", "");

        ResponseEntity<?> response = controller.createLiveEnvironment(input);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createLiveEnvironment_userIdAsString_works() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", "7");
        input.put("vncPassword", "pw");

        Map<String, Object> created = Map.of("id", 1, "vncPort", 5907);
        mockDbPost(created);

        ResponseEntity<?> response = controller.createLiveEnvironment(input);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void createLiveEnvironment_setsDefaultVncHost() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 2);
        input.put("vncPassword", "pw");

        Map<String, Object> created = Map.of("id", 1, "vncPort", 5902);
        mockDbPost(created);

        controller.createLiveEnvironment(input);

        assertThat(input.get("vncHost")).isEqualTo("localhost");
    }

    @Test
    void createLiveEnvironment_doesNotOverrideExistingVncHost() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 2);
        input.put("vncPassword", "pw");
        input.put("vncHost", "192.168.1.1");

        Map<String, Object> created = Map.of("id", 1, "vncPort", 5902);
        mockDbPost(created);

        controller.createLiveEnvironment(input);

        assertThat(input.get("vncHost")).isEqualTo("192.168.1.1");
    }

    @Test
    void createLiveEnvironment_setsDefaultStatus() {
        Map<String, Object> input = new HashMap<>();
        input.put("userId", 3);
        input.put("vncPassword", "pw");

        Map<String, Object> created = Map.of("id", 1, "vncPort", 5903);
        mockDbPost(created);

        controller.createLiveEnvironment(input);

        assertThat(input.get("status")).isEqualTo("stopped");
    }

    // ===================== getVncPortByUserId =====================

    @Test
    void getVncPortByUserId_returnsVncPort() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);
        liveEnv.put("vncPassword", "pw");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(liveEnv));

        ResponseEntity<?> response = controller.getVncPortByUserId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("vncPort");
        assertThat(body.get("vncPort")).isEqualTo(5901);
        assertThat(body.get("vncPassword")).isEqualTo("pw");
    }

    @Test
    void getVncPortByUserId_notFound_returns404() {
        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.empty());

        ResponseEntity<?> response = controller.getVncPortByUserId(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getVncPortByUserId_noVncPort_returns404() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(liveEnv));

        ResponseEntity<?> response = controller.getVncPortByUserId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getVncPortByUserId_withoutPasswordStillReturnsPort() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5903);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(liveEnv));

        ResponseEntity<?> response = controller.getVncPortByUserId(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsKey("vncPort");
        assertThat(body).doesNotContainKey("vncPassword");
    }

    // ===================== startLiveEnvironment =====================

    @Test
    void startLiveEnvironment_existingEnv_startsSuccessfully() throws Exception {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);
        liveEnv.put("status", "stopped");

        Map<String, Object> user = Map.of("name", "MaxMuster");
        Map<String, Object> updatedEnv = new HashMap<>(liveEnv);
        updatedEnv.put("status", "running");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        mockBackendPost(Map.of("status", "ok"));
        mockDbPut(updatedEnv);
        doNothing().when(wsHandler).sendToUser(any(), any());

        ResponseEntity<?> response = controller.startLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wsHandler).sendToUser(eq(1L), any());
    }

    @Test
    void startLiveEnvironment_userNotFound_returnsBadRequest() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.empty()); // user → null

        ResponseEntity<?> response = controller.startLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void startLiveEnvironment_backendError_returnsBadGateway() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);
        Map<String, Object> user = Map.of("name", "Max");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        when(backendWebClient.post()).thenReturn(bePostUriSpec);
        when(bePostUriSpec.uri(anyString())).thenReturn(bePostBodySpec);
        when(bePostBodySpec.bodyValue(any())).thenReturn(bePostHeadersSpec);
        when(bePostHeadersSpec.retrieve()).thenThrow(new RuntimeException("backend down"));

        ResponseEntity<?> response = controller.startLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void startLiveEnvironment_newEnvCreated_whenNoneExists() {
        Map<String, Object> createdEnv = new HashMap<>();
        createdEnv.put("id", 10);
        createdEnv.put("vncPort", 5902);
        createdEnv.put("status", "running");

        Map<String, Object> user = Map.of("name", "TestUser");
        Map<String, Object> updatedEnv = new HashMap<>(createdEnv);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.empty())           // kein live-env
                .thenReturn(Mono.just(user));        // user
        when(dbGetResponseSpec.bodyToMono(eq(Integer.class)))
                .thenReturn(Mono.just(5901));        // max-vnc-port

        mockDbPost(createdEnv);
        mockBackendPost(Map.of("status", "ok"));
        mockDbPut(updatedEnv);
        doNothing().when(wsHandler).sendToUser(any(), any());

        ResponseEntity<?> response = controller.startLiveEnvironment(2L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ===================== stopLiveEnvironment =====================

    @Test
    void stopLiveEnvironment_noLiveEnvFound_returnsBadRequest() {
        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.empty());

        ResponseEntity<?> response = controller.stopLiveEnvironment(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void stopLiveEnvironment_liveEnvWithoutId_returnsBadRequest() {
        Map<String, Object> liveEnv = new HashMap<>();

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(liveEnv));

        ResponseEntity<?> response = controller.stopLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void stopLiveEnvironment_success_returns200() throws Exception {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);

        Map<String, Object> user = Map.of("name", "MaxMuster");
        Map<String, Object> updatedEnv = new HashMap<>(liveEnv);
        updatedEnv.put("status", "stopped");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        mockBackendPost(Map.of("status", "ok"));
        mockDbPut(updatedEnv);
        doNothing().when(wsHandler).sendToUser(any(), any());

        ResponseEntity<?> response = controller.stopLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wsHandler).sendToUser(eq(1L), any());
    }

    @Test
    void stopLiveEnvironment_userNotFound_returnsBadRequest() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.empty());

        ResponseEntity<?> response = controller.stopLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void stopLiveEnvironment_backendError_returnsBadGateway() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        Map<String, Object> user = Map.of("name", "Max");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        when(backendWebClient.post()).thenReturn(bePostUriSpec);
        when(bePostUriSpec.uri(anyString())).thenReturn(bePostBodySpec);
        when(bePostBodySpec.bodyValue(any())).thenReturn(bePostHeadersSpec);
        when(bePostHeadersSpec.retrieve()).thenThrow(new RuntimeException("backend down"));

        ResponseEntity<?> response = controller.stopLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    // ===================== resetLiveEnvironment =====================

    @Test
    void resetLiveEnvironment_noLiveEnvFound_returnsBadRequest() {
        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.empty());

        ResponseEntity<?> response = controller.resetLiveEnvironment(99L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetLiveEnvironment_liveEnvWithoutId_returnsBadRequest() {
        Map<String, Object> liveEnv = new HashMap<>();

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class))).thenReturn(Mono.just(liveEnv));

        ResponseEntity<?> response = controller.resetLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetLiveEnvironment_success_returns200() throws Exception {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        liveEnv.put("vncPort", 5901);

        Map<String, Object> user = Map.of("name", "MaxMuster");
        Map<String, Object> updatedEnv = new HashMap<>(liveEnv);
        updatedEnv.put("status", "running");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        mockBackendPost(Map.of("status", "ok"));
        mockDbPut(updatedEnv);
        doNothing().when(wsHandler).sendToUser(any(), any());

        ResponseEntity<?> response = controller.resetLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(wsHandler).sendToUser(eq(1L), any());
    }

    @Test
    void resetLiveEnvironment_userNotFound_returnsBadRequest() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.empty());

        ResponseEntity<?> response = controller.resetLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void resetLiveEnvironment_backendError_returnsBadGateway() {
        Map<String, Object> liveEnv = new HashMap<>();
        liveEnv.put("id", 1);
        Map<String, Object> user = Map.of("name", "Max");

        when(databaseWebClient.get()).thenReturn(dbGetUriSpec);
        when(dbGetUriSpec.uri(anyString())).thenReturn(dbGetHeadersSpec);
        when(dbGetHeadersSpec.retrieve()).thenReturn(dbGetResponseSpec);
        when(dbGetResponseSpec.bodyToMono(eq(Map.class)))
                .thenReturn(Mono.just(liveEnv))
                .thenReturn(Mono.just(user));

        when(backendWebClient.post()).thenReturn(bePostUriSpec);
        when(bePostUriSpec.uri(anyString())).thenReturn(bePostBodySpec);
        when(bePostBodySpec.bodyValue(any())).thenReturn(bePostHeadersSpec);
        when(bePostHeadersSpec.retrieve()).thenThrow(new RuntimeException("backend down"));

        ResponseEntity<?> response = controller.resetLiveEnvironment(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }
}

