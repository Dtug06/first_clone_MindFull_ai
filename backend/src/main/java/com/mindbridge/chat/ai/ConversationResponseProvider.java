package com.mindbridge.chat.ai;

/** Generates a non-emergency conversational response from redacted history. */
public interface ConversationResponseProvider {

    String generate(ConversationResponseInput input);
}
