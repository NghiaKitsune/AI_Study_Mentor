package com.studymentor.app.api;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertFalse;

/**
 * Contract tests for the request model used by the final Gemini integration.
 * These tests add report evidence without changing application behaviour.
 */
public class ChatRequestContractTest {

    @Test
    public void blankRequestIdGeneratesStableNonEmptyIdAndLegacyAlias() {
        ChatRequest request = new ChatRequest("   ", "Explain photosynthesis");

        assertNotNull(request.requestId);
        assertFalse(request.requestId.trim().isEmpty());
        assertEquals(request.requestId, request.request_id);
    }

    @Test
    public void nullMessageIsNormalizedToEmptyString() {
        ChatRequest request = new ChatRequest("req-001", null);

        assertEquals("req-001", request.requestId);
        assertEquals("", request.message);
    }

    @Test
    public void legacyConversationIdIsOnlyKeptWhenPositive() {
        ChatRequest valid = new ChatRequest("What is Room?", 15L);
        ChatRequest invalid = new ChatRequest("What is Room?", 0L);

        assertEquals(Long.valueOf(15L), valid.conversation_id);
        assertNull(invalid.conversation_id);
    }
}
