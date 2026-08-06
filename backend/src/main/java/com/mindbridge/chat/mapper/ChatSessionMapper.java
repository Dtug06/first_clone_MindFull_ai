package com.mindbridge.chat.mapper;

import com.mindbridge.chat.domain.ChatSession;
import com.mindbridge.chat.dto.ChatSessionResponse;
import com.mindbridge.chat.dto.ChatSessionResponse.ChatSessionStatus;
import org.springframework.stereotype.Component;

/**
 * Maps ChatSession entity to ChatSessionResponse DTO.
 * No business logic — only field mapping.
 */
@Component
public class ChatSessionMapper {

    /**
     * Maps a ChatSession entity to the API response shape.
     * Maps the domain ChatSessionStatus enum to the response's ChatSessionStatus enum
     * (same name, same type, but defined in the DTO record for API isolation).
     */
    public ChatSessionResponse toResponse(ChatSession entity) {
        return new ChatSessionResponse(
                entity.getId(),
                entity.getTitle(),
                ChatSessionStatus.valueOf(entity.getStatus().name()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
