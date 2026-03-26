package itsi.api.database.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ApiInfoControllerTest {

    @InjectMocks
    private ApiInfoController apiInfoController;

    @Test
    void getApiInfoShouldReturnOk() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getApiInfoShouldContainName() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertEquals("Database API", response.getBody().get("name"));
    }

    @Test
    void getApiInfoShouldContainVersion() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertEquals("1.0.0", response.getBody().get("version"));
    }

    @Test
    void getApiInfoShouldContainEndpoints() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("swagger_ui"));
        assertTrue(response.getBody().containsKey("api_docs"));
    }
}

