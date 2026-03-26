package itsi.api.database.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ApiInfoControllerTest {

    @InjectMocks
    private ApiInfoController apiInfoController;

    @Test
    void getApiInfo_shouldReturnOk() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void getApiInfo_shouldContainName() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertEquals("Database API", response.getBody().get("name"));
    }

    @Test
    void getApiInfo_shouldContainVersion() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertEquals("1.0.0", response.getBody().get("version"));
    }

    @Test
    void getApiInfo_shouldContainEndpoints() {
        ResponseEntity<Map<String, Object>> response = apiInfoController.getApiInfo();

        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("swagger_ui"));
        assertTrue(response.getBody().containsKey("api_docs"));
    }
}

