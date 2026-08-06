package com.mindbridge.auth.service;

import com.mindbridge.auth.domain.entity.User;
import com.mindbridge.auth.dto.AuthResponse;
import com.mindbridge.auth.dto.LoginRequest;
import com.mindbridge.auth.dto.RegisterRequest;
import com.mindbridge.auth.exception.DuplicateEmailException;
import com.mindbridge.auth.mapper.UserMapper;
import com.mindbridge.auth.repository.UserRepository;
import com.mindbridge.common.audit.AuditActorType;
import com.mindbridge.common.audit.AuditActions;
import com.mindbridge.common.audit.AuditCategory;
import com.mindbridge.common.audit.AuditService;
import com.mindbridge.common.audit.LogSanitizer;
import com.mindbridge.common.exception.ErrorCode;
import com.mindbridge.common.exception.MindBridgeException;
import com.mindbridge.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user registration and login.
 *
 * Security rules enforced here:
 * - Passwords are never logged.
 * - Token values are never logged.
 * - Login failure never distinguishes "bad password" from "user not found".
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                      PasswordEncoder passwordEncoder,
                      JwtService jwtService,
                      UserMapper userMapper,
                      AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.auditService = auditService;
    }

    /**
     * Registers a new user account.
     *
     * @param request valid registration data
     * @return AuthResponse with access token and user profile
     * @throws DuplicateEmailException if the email is already taken
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        String displayName = request.displayName().trim();
        String passwordHash = passwordEncoder.encode(request.password());

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateEmailException(request.email());
        }

        User user = User.register(email, passwordHash, displayName);
        user = userRepository.save(user);

        UserResponse userResponse = userMapper.toResponse(user);
        String token = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userResponse
        );
    }

    /**
     * Authenticates a user and returns an access token.
     *
     * @param request login credentials
     * @return AuthResponse with access token and user profile
     * @throws MindBridgeException with AUTH_CREDENTIALS_INVALID on bad email or password
     * @throws MindBridgeException with USER_SUSPENDED if account is suspended
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        String emailHash = LogSanitizer.sha256Hex(email);

        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            recordLoginFailure(emailHash);
            throw new MindBridgeException(
                    ErrorCode.AUTH_CREDENTIALS_INVALID,
                    "Email or password is incorrect");
        }

        if (user.getStatus() == User.UserStatus.SUSPENDED) {
            recordLoginFailure(emailHash);
            throw new MindBridgeException(ErrorCode.USER_SUSPENDED);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            recordLoginFailure(emailHash);
            throw new MindBridgeException(ErrorCode.AUTH_CREDENTIALS_INVALID,
                    "Email or password is incorrect");
        }

        UserResponse userResponse = userMapper.toResponse(user);
        String token = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );

        return new AuthResponse(
                token,
                "Bearer",
                jwtService.getAccessTokenExpirationMs(),
                userResponse
        );
    }

    private void recordLoginFailure(String emailHash) {
        String meta = "{\"emailHash\":\"" + emailHash + "\"}";
        auditService.record(AuditCategory.AUTH, AuditActions.LOGIN_FAILED,
                AuditActorType.ANONYMOUS, null, "user", null, meta);
    }
}
