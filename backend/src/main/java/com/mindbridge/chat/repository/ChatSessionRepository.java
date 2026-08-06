package com.mindbridge.chat.repository;

import com.mindbridge.chat.domain.ChatSession;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for chat sessions.
 *
 * Query patterns:
 * - List sessions for a user ordered by most recently active (updated_at DESC).
 * - Fetch a single session by id for ownership verification.
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, UUID> {

    /**
     * Returns all sessions for a user ordered by updated_at DESC (most recently active first).
     */
    Page<ChatSession> findByUserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    /**
     * Finds a session by id and user id. Used for ownership verification before
     * returning a session to the caller.
     */
    Optional<ChatSession> findByIdAndUserId(UUID id, UUID userId);
}
