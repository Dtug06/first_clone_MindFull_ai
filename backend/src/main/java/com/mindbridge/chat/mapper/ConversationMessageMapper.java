package com.mindbridge.chat.mapper;

import com.mindbridge.chat.domain.ConversationMessage;
import com.mindbridge.chat.dto.ChatMessageResponse;
import com.mindbridge.chat.dto.ChatMessageResponse.AnalysisStatus;
import com.mindbridge.chat.dto.ChatMessageResponse.MessageRole;
import org.springframework.stereotype.Component;

/**
 * Maps ConversationMessage entity to ChatMessageResponse DTO.
 * No business logic — only field mapping.
 */
@Component
public class ConversationMessageMapper {

    /**
     * Maps a ConversationMessage entity to the API response shape.
     * Maps domain enums to response enums (same name, different type).
     * analysisStatus is always NOT_REQUESTED for user-sent messages;
     * pipeline status updates are handled by a separate process (G2-T04).
     */
    public ChatMessageResponse toResponse(ConversationMessage entity) {
        return toResponse(entity, AnalysisStatus.NOT_REQUESTED);
    }

    public ChatMessageResponse toResponse(
            ConversationMessage entity,
            AnalysisStatus analysisStatus) {
        return new ChatMessageResponse(
                entity.getId(),
                entity.getSessionId(),
                MessageRole.valueOf(entity.getRole().name()),
                entity.getContent(),
                entity.getCreatedAt(),
                analysisStatus
        );
    }
}
