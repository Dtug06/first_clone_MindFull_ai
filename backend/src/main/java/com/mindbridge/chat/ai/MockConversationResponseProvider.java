package com.mindbridge.chat.ai;

/** Deterministic offline provider for local development and automated tests. */
public final class MockConversationResponseProvider implements ConversationResponseProvider {

    @Override
    public String generate(ConversationResponseInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }
        return "[DEMO_ONLY] Thank you for sharing. What would feel most helpful to explore next?";
    }
}
