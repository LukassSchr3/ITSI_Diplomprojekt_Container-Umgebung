package itsi.api.database.repository;

import itsi.api.database.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
class UserRepositoryTest {

    @Mock
    private UserRepository userRepository;

    @Test
    void saveShouldPersistUser() {
        User testUser = new User();
        testUser.setName("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password123");
        testUser.setClassName("5AHIT");
        testUser.setRole("USER");
        testUser.setCreatedAt(new Timestamp(System.currentTimeMillis()));

        User savedUser = new User();
        savedUser.setId(1);
        savedUser.setName("testuser");
        savedUser.setEmail("test@example.com");
        savedUser.setPassword("password123");
        savedUser.setClassName("5AHIT");
        savedUser.setRole("USER");
        savedUser.setCreatedAt(testUser.getCreatedAt());

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User saved = userRepository.save(testUser);

        assertNotNull(saved);
        assertNotNull(saved.getId());
        assertEquals(testUser.getName(), saved.getName());
        assertEquals(testUser.getEmail(), saved.getEmail());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void findByIdShouldReturnUserWhenExists() {
        User testUser = new User();
        testUser.setId(1);
        testUser.setName("testuser");

        when(userRepository.findById(1)).thenReturn(Optional.of(testUser));

        Optional<User> found = userRepository.findById(1);

        assertTrue(found.isPresent());
        assertEquals(1, found.get().getId());
        assertEquals("testuser", found.get().getName());
        verify(userRepository, times(1)).findById(1);
    }

    @Test
    void findByIdShouldReturnEmptyWhenNotExists() {
        when(userRepository.findById(9999)).thenReturn(Optional.empty());

        Optional<User> found = userRepository.findById(9999);

        assertFalse(found.isPresent());
        verify(userRepository, times(1)).findById(9999);
    }

    @Test
    void findByNameShouldReturnUserWhenExists() {
        User testUser = new User();
        testUser.setName("testuser");
        testUser.setEmail("test@example.com");

        when(userRepository.findByName("testuser")).thenReturn(Optional.of(testUser));

        Optional<User> found = userRepository.findByName("testuser");

        assertTrue(found.isPresent());
        assertEquals("testuser", found.get().getName());
        assertEquals("test@example.com", found.get().getEmail());
        verify(userRepository, times(1)).findByName("testuser");
    }

    @Test
    void findByNameShouldReturnEmptyWhenNotExists() {
        when(userRepository.findByName("nonexistent")).thenReturn(Optional.empty());

        Optional<User> found = userRepository.findByName("nonexistent");

        assertFalse(found.isPresent());
        verify(userRepository, times(1)).findByName("nonexistent");
    }

    @Test
    void findByEmailShouldReturnUserWhenExists() {
        User testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setName("testuser");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        Optional<User> found = userRepository.findByEmail("test@example.com");

        assertTrue(found.isPresent());
        assertEquals("test@example.com", found.get().getEmail());
        assertEquals("testuser", found.get().getName());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void findByEmailShouldReturnEmptyWhenNotExists() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

        assertFalse(found.isPresent());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void findAllShouldReturnAllUsers() {
        User user1 = new User();
        user1.setName("testuser1");

        User user2 = new User();
        user2.setName("testuser2");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<User> users = userRepository.findAll();

        assertNotNull(users);
        assertEquals(2, users.size());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void findAllShouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        List<User> users = userRepository.findAll();

        assertNotNull(users);
        assertTrue(users.isEmpty());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void deleteByIdShouldRemoveUser() {
        doNothing().when(userRepository).deleteById(1);

        userRepository.deleteById(1);

        verify(userRepository, times(1)).deleteById(1);
    }

    @Test
    void updateShouldModifyExistingUser() {
        User existingUser = new User();
        existingUser.setId(1);
        existingUser.setName("oldname");
        existingUser.setEmail("old@example.com");

        User updatedUser = new User();
        updatedUser.setId(1);
        updatedUser.setName("updatedname");
        updatedUser.setEmail("updated@example.com");

        when(userRepository.save(any(User.class))).thenReturn(updatedUser);

        User result = userRepository.save(existingUser);

        assertNotNull(result);
        assertEquals(1, result.getId());
        assertEquals("updatedname", result.getName());
        assertEquals("updated@example.com", result.getEmail());
        verify(userRepository, times(1)).save(existingUser);
    }
}
