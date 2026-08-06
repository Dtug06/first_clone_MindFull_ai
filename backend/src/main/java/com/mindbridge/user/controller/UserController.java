package com.mindbridge.user.controller;

import com.mindbridge.auth.mapper.UserMapper;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.common.exception.ResourceNotFoundException;
import com.mindbridge.common.service.CurrentUserService;
import com.mindbridge.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * User controller — returns the authenticated user's own profile.
 *
 * All userId values come from CurrentUserService (backed by JWT principal) —
 * never trust a client-provided userId.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    public UserController(UserRepository userRepository,
                         UserMapper userMapper,
                         CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.currentUserService = currentUserService;
    }

    /**
     * GET /users/me
     * Returns the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        var user = userRepository.findById(currentUserService.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserService.getCurrentUserId()));
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
}
