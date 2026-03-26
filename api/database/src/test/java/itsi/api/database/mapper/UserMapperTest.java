package itsi.api.database.mapper;

import itsi.api.database.dto.CreateUserDTO;
import itsi.api.database.dto.UpdateUserDTO;
import itsi.api.database.dto.UserDTO;
import itsi.api.database.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    void toDTOShouldConvertUserToUserDTO() {
        User user = new User();
        user.setId(1);
        user.setName("testuser");
        user.setEmail("test@example.com");
        user.setPassword("secretPassword");
        user.setClassName("5AHIT");
        user.setRole("ADMIN");
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setExpiredAt(new Timestamp(System.currentTimeMillis() + 86400000));

        UserDTO dto = userMapper.toDTO(user);

        assertNotNull(dto);
        assertEquals(user.getId(), dto.getId());
        assertEquals(user.getName(), dto.getName());
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getClassName(), dto.getClassName());
        assertEquals(user.getRole(), dto.getRole());
        assertEquals(user.getCreatedAt(), dto.getCreatedAt());
        assertEquals(user.getExpiredAt(), dto.getExpiredAt());
    }

    @Test
    void toDTOShouldReturnNullForNullUser() {
        UserDTO dto = userMapper.toDTO(null);
        assertNull(dto);
    }

    @Test
    void toEntityShouldConvertCreateUserDTOToUser() {
        CreateUserDTO createDTO = new CreateUserDTO();
        createDTO.setName("newuser");
        createDTO.setEmail("new@example.com");
        createDTO.setPassword("password123");
        createDTO.setClassName("5BHIT");
        createDTO.setRole("USER");
        createDTO.setExpiredAt(new Timestamp(System.currentTimeMillis() + 86400000));

        User user = userMapper.toEntity(createDTO);

        assertNotNull(user);
        assertNull(user.getId());
        assertEquals(createDTO.getName(), user.getName());
        assertEquals(createDTO.getEmail(), user.getEmail());
        assertEquals(createDTO.getPassword(), user.getPassword());
        assertEquals(createDTO.getClassName(), user.getClassName());
        assertEquals(createDTO.getRole(), user.getRole());
        assertNotNull(user.getCreatedAt());
        assertEquals(createDTO.getExpiredAt(), user.getExpiredAt());
    }

    @Test
    void toEntityShouldReturnNullForNullDTO() {
        User user = userMapper.toEntity(null);
        assertNull(user);
    }

    @Test
    void updateEntityShouldUpdateAllFields() {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setName("oldname");
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("oldpassword");
        existingUser.setClassName("4AHIT");
        existingUser.setRole("USER");
        existingUser.setCreatedAt(new Timestamp(System.currentTimeMillis() - 86400000));
        existingUser.setExpiredAt(new Timestamp(System.currentTimeMillis()));

        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setName("newname");
        updateDTO.setEmail("new@example.com");
        updateDTO.setPassword("newpassword");
        updateDTO.setClassName("5AHIT");
        updateDTO.setRole("ADMIN");
        updateDTO.setExpiredAt(new Timestamp(System.currentTimeMillis() + 86400000));

        userMapper.updateEntity(existingUser, updateDTO);

        assertEquals(1, existingUser.getId());
        assertEquals("newname", existingUser.getName());
        assertEquals("new@example.com", existingUser.getEmail());
        assertEquals("newpassword", existingUser.getPassword());
        assertEquals("5AHIT", existingUser.getClassName());
        assertEquals("ADMIN", existingUser.getRole());
        assertEquals(updateDTO.getExpiredAt(), existingUser.getExpiredAt());
    }

    @Test
    void updateEntityShouldOnlyUpdateNonNullFields() {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setName("oldname");
        existingUser.setEmail("old@example.com");
        existingUser.setPassword("oldpassword");
        existingUser.setClassName("4AHIT");
        existingUser.setRole("USER");

        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setName("newname");

        userMapper.updateEntity(existingUser, updateDTO);

        assertEquals("newname", existingUser.getName());
        assertEquals("old@example.com", existingUser.getEmail());
        assertEquals("oldpassword", existingUser.getPassword());
        assertEquals("4AHIT", existingUser.getClassName());
        assertEquals("USER", existingUser.getRole());
    }

    @Test
    void updateEntityShouldNotUpdatePasswordIfEmpty() {
        User existingUser = new User();
        existingUser.setPassword("oldpassword");

        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setPassword("");

        userMapper.updateEntity(existingUser, updateDTO);

        assertEquals("oldpassword", existingUser.getPassword());
    }

    @Test
    void updateEntityShouldHandleNullUserGracefully() {
        UpdateUserDTO updateDTO = new UpdateUserDTO();
        updateDTO.setName("newname");

        assertDoesNotThrow(() -> userMapper.updateEntity(null, updateDTO));
    }

    @Test
    void updateEntityShouldHandleNullDTOGracefully() {
        User existingUser = new User();
        existingUser.setName("oldname");

        assertDoesNotThrow(() -> userMapper.updateEntity(existingUser, null));
        assertEquals("oldname", existingUser.getName());
    }
}

