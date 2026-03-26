package itsi.api.steuerung.dto;

import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;

class DtoTest {

    // ===================== ImageDTO =====================

    @Test
    void imageDto_noArgsConstructorCreatesEmptyObject() {
        ImageDTO dto = new ImageDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getImageRef()).isNull();
    }

    @Test
    void imageDto_allArgsConstructorSetsAllFields() {
        ImageDTO dto = new ImageDTO(1, "ubuntu", "ubuntu:22.04");
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getName()).isEqualTo("ubuntu");
        assertThat(dto.getImageRef()).isEqualTo("ubuntu:22.04");
    }

    @Test
    void imageDto_settersWork() {
        ImageDTO dto = new ImageDTO();
        dto.setId(5);
        dto.setName("alpine");
        dto.setImageRef("alpine:3.18");
        assertThat(dto.getId()).isEqualTo(5);
        assertThat(dto.getName()).isEqualTo("alpine");
        assertThat(dto.getImageRef()).isEqualTo("alpine:3.18");
    }

    @Test
    void imageDto_equalityBasedOnFields() {
        ImageDTO a = new ImageDTO(1, "ubuntu", "ubuntu:22.04");
        ImageDTO b = new ImageDTO(1, "ubuntu", "ubuntu:22.04");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void imageDto_toStringContainsFields() {
        ImageDTO dto = new ImageDTO(3, "debian", "debian:11");
        assertThat(dto.toString()).contains("debian");
    }

    // ===================== UserDTO =====================

    @Test
    void userDto_noArgsConstructorCreatesEmptyObject() {
        UserDTO dto = new UserDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getEmail()).isNull();
    }

    @Test
    void userDto_allArgsConstructorSetsAllFields() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        UserDTO dto = new UserDTO(1, "Max", "max@test.at", "pw", "5AHIT", "SCHUELER", now, now);
        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getName()).isEqualTo("Max");
        assertThat(dto.getEmail()).isEqualTo("max@test.at");
        assertThat(dto.getPassword()).isEqualTo("pw");
        assertThat(dto.getClassName()).isEqualTo("5AHIT");
        assertThat(dto.getRole()).isEqualTo("SCHUELER");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getExpiredAt()).isEqualTo(now);
    }

    @Test
    void userDto_settersWork() {
        UserDTO dto = new UserDTO();
        dto.setId(7);
        dto.setName("Anna");
        dto.setEmail("anna@test.at");
        dto.setRole("LEHRER");
        assertThat(dto.getId()).isEqualTo(7);
        assertThat(dto.getName()).isEqualTo("Anna");
        assertThat(dto.getEmail()).isEqualTo("anna@test.at");
        assertThat(dto.getRole()).isEqualTo("LEHRER");
    }

    @Test
    void userDto_equalityBasedOnFields() {
        UserDTO a = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        UserDTO b = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        assertThat(a).isEqualTo(b);
    }

    // ===================== InstanceDTO =====================

    @Test
    void instanceDto_noArgsConstructorCreatesEmptyObject() {
        InstanceDTO dto = new InstanceDTO();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getContainerId()).isNull();
        assertThat(dto.getImage()).isNull();
        assertThat(dto.getUser()).isNull();
    }

    @Test
    void instanceDto_allArgsConstructorSetsAllFields() {
        ImageDTO img = new ImageDTO(2, "ubuntu", "ubuntu:22.04");
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO dto = new InstanceDTO(10, "cont_1", "ubuntu_Max", img, user, "running");
        assertThat(dto.getId()).isEqualTo(10);
        assertThat(dto.getContainerId()).isEqualTo("cont_1");
        assertThat(dto.getName()).isEqualTo("ubuntu_Max");
        assertThat(dto.getImage()).isEqualTo(img);
        assertThat(dto.getUser()).isEqualTo(user);
        assertThat(dto.getStatus()).isEqualTo("running");
    }

    @Test
    void instanceDto_getImageIdReturnsImageId() {
        ImageDTO img = new ImageDTO(5, "alpine", "alpine:3");
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", img, null, "stopped");
        assertThat(dto.getImageId()).isEqualTo(5);
    }

    @Test
    void instanceDto_getImageIdNullWhenNoImage() {
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", null, null, "stopped");
        assertThat(dto.getImageId()).isNull();
    }

    @Test
    void instanceDto_getUserIdReturnsUserId() {
        UserDTO user = new UserDTO(3, "Anna", "a@t.at", null, "4A", "LEHRER", null, null);
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", null, user, "running");
        assertThat(dto.getUserId()).isEqualTo(3);
    }

    @Test
    void instanceDto_getUserIdNullWhenNoUser() {
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", null, null, "running");
        assertThat(dto.getUserId()).isNull();
    }

    @Test
    void instanceDto_setImageIdCreatesImageIfNull() {
        InstanceDTO dto = new InstanceDTO();
        dto.setImageId(9);
        assertThat(dto.getImage()).isNotNull();
        assertThat(dto.getImage().getId()).isEqualTo(9);
    }

    @Test
    void instanceDto_setUserIdCreatesUserIfNull() {
        InstanceDTO dto = new InstanceDTO();
        dto.setUserId(4);
        assertThat(dto.getUser()).isNotNull();
        assertThat(dto.getUser().getId()).isEqualTo(4);
    }

    @Test
    void instanceDto_setImageIdUpdatesExistingImage() {
        ImageDTO img = new ImageDTO(1, "ubuntu", "ubuntu:22");
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", img, null, "running");
        dto.setImageId(99);
        assertThat(dto.getImage().getId()).isEqualTo(99);
    }

    @Test
    void instanceDto_setUserIdUpdatesExistingUser() {
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        InstanceDTO dto = new InstanceDTO(1, "c1", "n1", null, user, "running");
        dto.setUserId(42);
        assertThat(dto.getUser().getId()).isEqualTo(42);
    }

    @Test
    void instanceDto_settersWork() {
        InstanceDTO dto = new InstanceDTO();
        dto.setId(7);
        dto.setContainerId("cont_7");
        dto.setName("test_name");
        dto.setStatus("stopped");
        assertThat(dto.getId()).isEqualTo(7);
        assertThat(dto.getContainerId()).isEqualTo("cont_7");
        assertThat(dto.getName()).isEqualTo("test_name");
        assertThat(dto.getStatus()).isEqualTo("stopped");
    }

    // ===================== ContainerOperationRequest =====================

    @Test
    void containerOperationRequest_noArgsConstructor() {
        ContainerOperationRequest req = new ContainerOperationRequest();
        assertThat(req.getUserId()).isNull();
        assertThat(req.getImageId()).isNull();
    }

    @Test
    void containerOperationRequest_allArgsConstructor() {
        ContainerOperationRequest req = new ContainerOperationRequest(3, 7);
        assertThat(req.getUserId()).isEqualTo(3);
        assertThat(req.getImageId()).isEqualTo(7);
    }

    @Test
    void containerOperationRequest_setters() {
        ContainerOperationRequest req = new ContainerOperationRequest();
        req.setUserId(10);
        req.setImageId(20);
        assertThat(req.getUserId()).isEqualTo(10);
        assertThat(req.getImageId()).isEqualTo(20);
    }

    @Test
    void containerOperationRequest_equality() {
        ContainerOperationRequest a = new ContainerOperationRequest(1, 2);
        ContainerOperationRequest b = new ContainerOperationRequest(1, 2);
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    // ===================== ContainerOperationResponse =====================

    @Test
    void containerOperationResponse_noArgsConstructor() {
        ContainerOperationResponse resp = new ContainerOperationResponse();
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getMessage()).isNull();
        assertThat(resp.getContainerId()).isNull();
        assertThat(resp.getStatus()).isNull();
        assertThat(resp.getInstance()).isNull();
    }

    @Test
    void containerOperationResponse_allArgsConstructor() {
        InstanceDTO inst = new InstanceDTO(1, "c1", "n1", null, null, "running");
        ContainerOperationResponse resp = new ContainerOperationResponse(true, "ok", "c1", "running", inst);
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("ok");
        assertThat(resp.getContainerId()).isEqualTo("c1");
        assertThat(resp.getStatus()).isEqualTo("running");
        assertThat(resp.getInstance()).isEqualTo(inst);
    }

    @Test
    void containerOperationResponse_setters() {
        ContainerOperationResponse resp = new ContainerOperationResponse();
        resp.setSuccess(true);
        resp.setMessage("Test message");
        resp.setContainerId("cont_3");
        resp.setStatus("stopped");
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("Test message");
        assertThat(resp.getContainerId()).isEqualTo("cont_3");
        assertThat(resp.getStatus()).isEqualTo("stopped");
    }

    @Test
    void containerOperationResponse_equality() {
        ContainerOperationResponse a = new ContainerOperationResponse(true, "ok", "c1", "running", null);
        ContainerOperationResponse b = new ContainerOperationResponse(true, "ok", "c1", "running", null);
        assertThat(a).isEqualTo(b);
    }

    // ===================== LoginRequest =====================

    @Test
    void loginRequest_noArgsConstructor() {
        LoginRequest req = new LoginRequest();
        assertThat(req.getEmail()).isNull();
        assertThat(req.getPassword()).isNull();
    }

    @Test
    void loginRequest_allArgsConstructor() {
        LoginRequest req = new LoginRequest("a@b.com", "secret");
        assertThat(req.getEmail()).isEqualTo("a@b.com");
        assertThat(req.getPassword()).isEqualTo("secret");
    }

    @Test
    void loginRequest_setters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("x@y.at");
        req.setPassword("pw123");
        assertThat(req.getEmail()).isEqualTo("x@y.at");
        assertThat(req.getPassword()).isEqualTo("pw123");
    }

    @Test
    void loginRequest_equality() {
        LoginRequest a = new LoginRequest("e@e.at", "p");
        LoginRequest b = new LoginRequest("e@e.at", "p");
        assertThat(a).isEqualTo(b);
    }

    // ===================== LoginResponse =====================

    @Test
    void loginResponse_noArgsConstructor() {
        LoginResponse resp = new LoginResponse();
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getToken()).isNull();
        assertThat(resp.getUser()).isNull();
    }

    @Test
    void loginResponse_allArgsConstructor() {
        UserDTO user = new UserDTO(1, "Max", "m@t.at", null, "5A", "SCHUELER", null, null);
        LoginResponse resp = new LoginResponse(true, "Login successful", "tok123", user);
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("Login successful");
        assertThat(resp.getToken()).isEqualTo("tok123");
        assertThat(resp.getUser()).isEqualTo(user);
    }

    @Test
    void loginResponse_setters() {
        LoginResponse resp = new LoginResponse();
        resp.setSuccess(true);
        resp.setMessage("OK");
        resp.setToken("abc");
        assertThat(resp.isSuccess()).isTrue();
        assertThat(resp.getMessage()).isEqualTo("OK");
        assertThat(resp.getToken()).isEqualTo("abc");
    }

    @Test
    void loginResponse_failureResponse() {
        LoginResponse resp = new LoginResponse(false, "Invalid email or password", null, null);
        assertThat(resp.isSuccess()).isFalse();
        assertThat(resp.getToken()).isNull();
        assertThat(resp.getUser()).isNull();
        assertThat(resp.getMessage()).contains("Invalid");
    }
}

