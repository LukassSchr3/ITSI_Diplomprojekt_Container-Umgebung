package itsi.api.database.controller;

import itsi.api.database.dto.CreateUserDTO;
import itsi.api.database.dto.UpdateUserDTO;
import itsi.api.database.dto.UserDTO;
import itsi.api.database.entity.User;
import itsi.api.database.mapper.UserMapper;
import itsi.api.database.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserController userController;

    private User testUser;
    private UserDTO testUserDTO;
    private CreateUserDTO createUserDTO;
    private UpdateUserDTO updateUserDTO;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1);
        testUser.setName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setClassName("5AHIT");
        testUser.setRole("USER");
        testUser.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        testUserDTO = new UserDTO();
        testUserDTO.setId(1);
        testUserDTO.setName("testuser");
        testUserDTO.setEmail("test@example.com");
        testUserDTO.setClassName("5AHIT");
        testUserDTO.setRole("USER");
        testUserDTO.setCreatedAt(testUser.getCreatedAt());

        createUserDTO = new CreateUserDTO();
        createUserDTO.setName("newuser");
        createUserDTO.setEmail("new@example.com");
        createUserDTO.setPassword("password123");
        createUserDTO.setClassName("5BHIT");
        createUserDTO.setRole("USER");

        updateUserDTO = new UpdateUserDTO();
        updateUserDTO.setName("updateduser");
        updateUserDTO.setEmail("updated@example.com");
    }

    @Test
    void getAllUsers_shouldReturnListOfUsers() {
        when(userService.findAll()).thenReturn(Arrays.asList(testUser));
        when(userMapper.toDTO(any(User.class))).thenReturn(testUserDTO);

        ResponseEntity<List<UserDTO>> response = userController.getAllUsers();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(testUserDTO, response.getBody().get(0));
        verify(userService, times(1)).findAll();
    }

    @Test
    void getAllUsers_shouldReturnEmptyList() {
        when(userService.findAll()).thenReturn(Collections.emptyList());

        ResponseEntity<List<UserDTO>> response = userController.getAllUsers();

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
        verify(userService, times(1)).findAll();
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userService.findById(1)).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        ResponseEntity<UserDTO> response = userController.getUserById(1);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserDTO.getId(), response.getBody().getId());
        assertEquals(testUserDTO.getName(), response.getBody().getName());
        verify(userService, times(1)).findById(1);
    }

    @Test
    void getUserById_shouldReturn404WhenNotFound() {
        when(userService.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = userController.getUserById(999);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNull(response.getBody());
        verify(userService, times(1)).findById(999);
    }

    @Test
    void getUserByName_shouldReturnUser() {
        when(userService.findByName("testuser")).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        ResponseEntity<UserDTO> response = userController.getUserByName("testuser");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("testuser", response.getBody().getName());
        verify(userService, times(1)).findByName("testuser");
    }

    @Test
    void getUserByName_shouldReturn404WhenNotFound() {
        when(userService.findByName("nonexistent")).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = userController.getUserByName("nonexistent");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).findByName("nonexistent");
    }

    @Test
    void getUserByEmail_shouldReturnUser() {
        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        ResponseEntity<UserDTO> response = userController.getUserByEmail("test@example.com");

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("test@example.com", response.getBody().getEmail());
        verify(userService, times(1)).findByEmail("test@example.com");
    }

    @Test
    void getUserByEmail_shouldReturn404WhenNotFound() {
        when(userService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = userController.getUserByEmail("nonexistent@example.com");

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void createUser_shouldReturnCreatedUser() {
        when(userMapper.toEntity(any(CreateUserDTO.class))).thenReturn(testUser);
        when(userService.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);

        ResponseEntity<UserDTO> response = userController.createUser(createUserDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(testUserDTO.getId(), response.getBody().getId());
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_shouldReturnUpdatedUser() {
        when(userService.findById(1)).thenReturn(Optional.of(testUser));
        when(userService.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toDTO(testUser)).thenReturn(testUserDTO);
        doNothing().when(userMapper).updateEntity(any(User.class), any(UpdateUserDTO.class));

        ResponseEntity<UserDTO> response = userController.updateUser(1, updateUserDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        verify(userService, times(1)).findById(1);
        verify(userService, times(1)).save(any(User.class));
    }

    @Test
    void updateUser_shouldReturn404WhenNotFound() {
        when(userService.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = userController.updateUser(999, updateUserDTO);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).findById(999);
        verify(userService, never()).save(any(User.class));
    }

    @Test
    void deleteUser_shouldReturnNoContent() {
        when(userService.findById(1)).thenReturn(Optional.of(testUser));
        doNothing().when(userService).deleteById(1);

        ResponseEntity<Void> response = userController.deleteUser(1);

        assertNotNull(response);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userService, times(1)).findById(1);
        verify(userService, times(1)).deleteById(1);
    }

    @Test
    void deleteUser_shouldReturn404WhenNotFound() {
        when(userService.findById(999)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = userController.deleteUser(999);

        assertNotNull(response);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(userService, times(1)).findById(999);
        verify(userService, never()).deleteById(anyInt());
    }
}
