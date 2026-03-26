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
    void getApiInfo_returns200() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getApiInfo_bodyIsNotNull() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void getApiInfo_containsApiName() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("name");
        assertThat(body.get("name").toString()).contains("Container");
    }

    @Test
    void getApiInfo_containsVersion() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("version");
        assertThat(body.get("version")).isEqualTo("1.0.0");
    }

    @Test
    void getApiInfo_containsPort() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("port");
        assertThat(body.get("port")).isEqualTo(8080);
    }

    @Test
    void getApiInfo_containsEndpoints() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("endpoints");
    }

    @Test
    void getApiInfo_containsConnections() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("connections");
    }

    @Test
    void getApiInfo_connectionsContainsDatabaseUrl() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, String> connections = (Map<String, String>) body.get("connections");
        assertThat(connections).containsKey("database_api");
        assertThat(connections.get("database_api")).contains("5050");
    }

    @Test
    void getApiInfo_connectionsContainsBackendUrl() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, String> connections = (Map<String, String>) body.get("connections");
        assertThat(connections).containsKey("backend_controller");
        assertThat(connections.get("backend_controller")).contains("3030");
    }

    @Test
    void getApiInfo_endpointsContainerOperationsPresent() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        @SuppressWarnings("unchecked")
        Map<String, Object> endpoints = (Map<String, Object>) body.get("endpoints");
        assertThat(endpoints).containsKey("Container Operations");
    }

    @Test
    void getApiInfo_swaggerUiPresent() {
        Map<String, Object> body = apiInfoController.getApiInfo().getBody();
        assertThat(body).containsKey("swagger_ui");
        assertThat(body.get("swagger_ui").toString()).contains("swagger-ui");
    }

    // --- healthCheck ---

    @Test
    void healthCheck_returns200() {
        ResponseEntity<Map<String, String>> response = apiInfoController.healthCheck();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void healthCheck_statusIsUp() {
        Map<String, String> body = apiInfoController.healthCheck().getBody();
        assertThat(body).containsKey("status");
        assertThat(body.get("status")).isEqualTo("UP");
    }

    @Test
    void healthCheck_messageIsPresent() {
        Map<String, String> body = apiInfoController.healthCheck().getBody();
        assertThat(body).containsKey("message");
        assertThat(body.get("message")).isNotBlank();
    }

    @Test
    void healthCheck_bodyIsNotNull() {
        assertThat(apiInfoController.healthCheck().getBody()).isNotNull();
    }
}

