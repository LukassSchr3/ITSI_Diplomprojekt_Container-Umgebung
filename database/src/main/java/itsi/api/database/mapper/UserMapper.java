package itsi.api.database.mapper;

import itsi.api.database.dto.CreateUserDTO;
import itsi.api.database.dto.UpdateUserDTO;
import itsi.api.database.dto.UserDTO;
import itsi.api.database.entity.User;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

/**
 * Mapper for converting between User entities and DTOs
 */
@Component
public class UserMapper {

    /**
     * Convert User entity to UserDTO (without password)
     */
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setClassName(user.getClassName());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setExpiredAt(user.getExpiredAt());
        return dto;
    }

    /**
     * Convert CreateUserDTO to User entity
     */
    public User toEntity(CreateUserDTO dto) {
        if (dto == null) {
            return null;
        }

        User user = new User();
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // In production: hash the password!
        user.setClassName(dto.getClassName());
        user.setRole(dto.getRole());
        user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
        user.setExpiredAt(dto.getExpiredAt());
        return user;
    }

    /**
     * Update existing User entity with UpdateUserDTO
     */
    public void updateEntity(User user, UpdateUserDTO dto) {
        if (user == null || dto == null) {
            return;
        }

        if (dto.getName() != null) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(dto.getPassword()); // In production: hash the password!
        }
        if (dto.getClassName() != null) {
            user.setClassName(dto.getClassName());
        }
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }
        if (dto.getExpiredAt() != null) {
            user.setExpiredAt(dto.getExpiredAt());
        }
    }
}

