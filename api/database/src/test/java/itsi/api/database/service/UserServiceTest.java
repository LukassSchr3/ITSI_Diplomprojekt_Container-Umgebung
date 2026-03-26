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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void findAllShouldReturnAllUsers() {
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
    void findAllShouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> actualUsers = userService.findAll();

        assertNotNull(actualUsers);
        assertTrue(actualUsers.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findByIdShouldReturnUserWhenExists() {
        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findById(1);

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotExists() {
        when(userRepository.findById(999)).thenReturn(Optional.empty());

        Optional<User> result = userService.findById(999);

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(999);
    }

    @Test
    void findByNameShouldReturnUserWhenExists() {
        when(userRepository.findByName("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByName("testuser");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByName("testuser");
    }

    @Test
    void findByNameShouldReturnEmptyWhenNotExists() {
        when(userRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByName("nonexistent");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByName("nonexistent");
    }

    @Test
    void findByEmailShouldReturnUserWhenExists() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> result = userService.findByEmail("test@example.com");

        assertTrue(result.isPresent());
        assertEquals(testUser, result.get());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenNotExists() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void saveShouldSaveAndReturnUser() {
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals(testUser, result);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void saveShouldUpdateExistingUser() {
        testUser.setName("updatedname");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.save(testUser);

        assertNotNull(result);
        assertEquals("updatedname", result.getName());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void deleteByIdShouldDeleteUser() {
        doNothing().when(userRepository).deleteById(1);

        userService.deleteById(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void deleteByIdShouldNotThrowExceptionWhenUserNotExists() {
        doNothing().when(userRepository).deleteById(999);

        assertDoesNotThrow(() -> userService.deleteById(999));
        verify(userRepository, times(1)).deleteById(999);
    }
}
