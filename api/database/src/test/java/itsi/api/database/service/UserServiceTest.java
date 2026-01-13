package itsi.api.database.service;

import itsi.api.database.entity.User;
import itsi.api.database.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

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
    }

    @Test
    void findAll_shouldReturnAllUsers() {
        User user2 = new User();
        user2.setId(2);
        user2.setName("testuser2");
        user2.setEmail("test2@example.com");

        List<User> expectedUsers = Arrays.asList(testUser, user2);
        when(userRepository.findAll()).thenReturn(expectedUsers);

        List<User> actualUsers = userService.findAll();

        assertNotNull(actualUsers);
        assertEquals(2, actualUsers.size());
        assertEquals(expectedUsers, actualUsers);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> actualUsers = userService.findAll();

        assertNotNull(actualUsers);
        assertTrue(actualUsers.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findById_shouldReturnUserWhenExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999);

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(999);
    }

    @Test
    void findByName_shouldReturnUserWhenExists() {
        when(userRepository.findByName("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByName("testuser");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByName("testuser");
    }

    @Test
    void findByName_shouldReturnEmptyWhenNotExists() {
        when(userRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByName("nonexistent");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByName("nonexistent");
    }

    @Test
    void findByEmail_shouldReturnUserWhenExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_shouldReturnEmptyWhenNotExists() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void save_shouldSaveAndReturnUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void save_shouldUpdateExistingUser() {
        testUser.setName("updatedname");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals("updatedname", result.getName());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deleteById_shouldDeleteUser() {
        doNothing().when(userRepository).deleteById(1);

        userService.deleteById(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteById_shouldNotThrowExceptionWhenUserNotExists() {
        doNothing().when(userRepository).deleteById(999);

        assertDoesNotThrow(() -> userService.deleteById(999));
        verify(userRepository, times(1)).deleteById(999);
    }
}
