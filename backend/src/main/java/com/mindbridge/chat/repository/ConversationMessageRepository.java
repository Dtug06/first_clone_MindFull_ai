package com.mindbridge.chat.repository;

import com.mindbridge.chat.domain.ConversationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for conversation messages.
 *
 * Query patterns:
 * - List messages for a session ordered by created_at ASC (stable chronological order).
 * - Count messages for a session (used for pagination totalElements).
 */
@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    /**
     * Returns all messages for a session ordered by created_at ASC (oldest first).
     * Used for both pagination and history display.
     */
    Page<ConversationMessage> findBySessionIdOrderByCreatedAtAsc(UUID sessionId, Pageable pageable);

    /** Latest context window for conversational response generation. */
    List<ConversationMessage> findTop20BySessionIdOrderByCreatedAtDesc(UUID sessionId);
}
