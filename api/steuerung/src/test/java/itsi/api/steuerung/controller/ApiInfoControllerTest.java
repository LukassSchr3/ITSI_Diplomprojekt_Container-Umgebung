package itsi.api.steuerung.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ApiInfoControllerTest {

    private ApiInfoController apiInfoController;

    @BeforeEach
    void setUp() {
        apiInfoController = new ApiInfoController();
    }

    // --- getApiInfo ---

    @Test
    void getApiInfoReturns200() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getApiInfoBodyIsNotNull() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getApiInfoContainsApiName() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("name");
        assertThat(body.get("name").toString()).contains("Container");
    }

    @Test
    void getApiInfoContainsVersion() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("version");
        assertThat(body.get("version")).isEqualTo("1.0.0");
    }

    @Test
    void getApiInfoContainsPort() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("port");
        assertThat(body.get("port")).isEqualTo(8080);
    }

    @Test
    void getApiInfoContainsEndpoints() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("endpoints");
    }

    @Test
    void getApiInfoContainsConnections() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("connections");
    }

    @Test
    void getApiInfoConnectionsContainsDatabaseUrl() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, String> connections = (Map<String, String>) body.get("connections");
        assertThat(connections).containsKey("database_api");
        assertThat(connections.get("database_api")).contains("5050");
    }

    @Test
    void getApiInfoConnectionsContainsBackendUrl() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, String> connections = (Map<String, String>) body.get("connections");
        assertThat(connections).containsKey("backend_controller");
        assertThat(connections.get("backend_controller")).contains("3030");
    }

    @Test
    void getApiInfoEndpointsContainerOperationsPresent() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> endpoints = (Map<String, Object>) body.get("endpoints");
        assertThat(endpoints).containsKey("Container Operations");
    }

    @Test
    void getApiInfoSwaggerUiPresent() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("swagger_ui");
        assertThat(body.get("swagger_ui").toString()).contains("swagger-ui");
    }

    // --- healthCheck ---

    @Test
    void healthCheckReturns200() {
        ResponseEntity<Map<String, String>> response = apiInfoController.healthCheck();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthCheckStatusIsUp() {
        Map<String, String> body = apiInfoController.healthCheck().getBody();
        assertThat(body).containsKey("status");
        assertThat(body.get("status")).isEqualTo("UP");
    }

    @Test
    void healthCheckMessageIsPresent() {
        Map<String, String> body = apiInfoController.healthCheck().getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isNotBlank();
    }

    @Test
    void healthCheckBodyIsNotNull() {
        assertThat(apiInfoController.healthCheck().getBody()).isNotNull();
    }
}

