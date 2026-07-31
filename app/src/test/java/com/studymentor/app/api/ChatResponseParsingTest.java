package com.studymentor.app.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.gson.Gson;

import org.junit.Test;

public class ChatResponseParsingTest {

    @Test
    public void structuredGeminiResponse_mapsSnakeCaseFields() {
        String json = "{"
                + "\"reply\":\"Here is a concise explanation.\","
                + "\"final_answer\":\"Plants convert light into chemical energy.\","
                + "\"steps\":[{\"index\":1,\"title\":\"Capture light\",\"body\":\"Chlorophyll absorbs sunlight.\"}],"
                + "\"key_concepts\":[\"chlorophyll\"],"
                + "\"common_mistakes\":[],"
                + "\"alternative_approach\":\"Think of a solar-powered food factory.\","
                + "\"examples\":[],"
                + "\"follow_ups\":[\"Where does photosynthesis occur?\"]"
                + "}";

        ChatResponse response = new Gson().fromJson(json, ChatResponse.class);

        assertNotNull(response);
        assertEquals("Plants convert light into chemical energy.", response.finalAnswer);
        assertEquals("Where does photosynthesis occur?", response.followUps.get(0));
        assertEquals("Capture light", response.steps.get(0).title);
    }
}
