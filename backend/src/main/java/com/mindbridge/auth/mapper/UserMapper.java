package com.mindbridge.auth.mapper;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.user.dto.UserResponse;
import org.mapstruct.Mapper;

/**
 * Maps {@link User} JPA entity to {@link UserResponse} DTO.
 *
 * CRITICAL: This mapper intentionally omits passwordHash.
 * UserResponse is the public API surface and must never expose it.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    UserResponse toResponse(User user);
}
