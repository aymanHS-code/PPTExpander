package com.powerpoint.expander;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.ResponseFormat;
import io.github.sashirestela.openai.common.ResponseFormat.JsonSchema;
import io.github.sashirestela.openai.domain.chat.*;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OpenAIExpander {
    private static final Logger LOGGER = Logger.getLogger(OpenAIExpander.class.getName());
    private static final String OPENAI_API_KEY = "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";
    private static final SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(OPENAI_API_KEY).build();

    public static String expandSlideContents(List<String> slideContents) {
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.of("You are an AI assistant that expands on PowerPoint slide content, providing more details and explanations. Format the response for optimal listening, ensuring it's not too fast or too slow."));
        
        StringBuilder prompt = new StringBuilder("Expand on the following PowerPoint slide contents:\n\n");
        for (int i = 0; i < slideContents.size(); i++) {
            prompt.append("Slide ").append(i + 1).append(":\n").append(slideContents.get(i)).append("\n\n");
        }
        prompt.append("Format your response as a JSON object with keys 'slide1', 'slide2', etc., containing the expanded content for each slide.");
        
        messages.add(UserMessage.of(prompt.toString()));

        // Create the JSON schema for SlideExpansion
        JSONObject schema = new JSONObject();
        schema.put("type", "object");
        JSONObject properties = new JSONObject();
        for (int i = 1; i <= slideContents.size(); i++) {
            properties.put("slide" + i, new JSONObject().put("type", "string"));
        }
        schema.put("properties", properties);
        schema.put("required", properties.keySet());

        ChatRequest chatRequest = ChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(messages)
                .responseFormat(ResponseFormat.jsonSchema(JsonSchema.builder()
                .name("SlideExpansion")
                .schemaClass(SlideExpansion.class)
                .build()))
                .maxCompletionTokens(2000)
                .n(1)
                .build();

        try {
            var chatResponse = openAI.chatCompletions().create(chatRequest).join();
            String content = chatResponse.firstContent();
            LOGGER.info("Raw API response: " + content);
            
            // Attempt to parse the JSON response
            try {
                JSONObject jsonResponse = new JSONObject(content);
                JSONArray slidesArray = new JSONArray();
                for (int i = 0; i < slideContents.size(); i++) {
                    String key = "slide" + (i + 1);
                    if (jsonResponse.has(key)) {
                        JSONObject slideObject = new JSONObject();
                        slideObject.put("expandedContent", jsonResponse.getString(key));
                        slidesArray.put(slideObject);
                    }
                }
                JSONObject result = new JSONObject();
                result.put("slides", slidesArray);
                return result.toString();
            } catch (JSONException e) {
                LOGGER.warning("Failed to parse JSON response: " + e.getMessage());
                return createErrorJson("Invalid JSON response from API");
            }
        } catch (Exception e) {
            LOGGER.severe("Error in API call: " + e.getMessage());
            return createErrorJson("Error in API call: " + e.getMessage());
        }
    }

    private static String createErrorJson(String errorMessage) {
        JSONObject errorJson = new JSONObject();
        errorJson.put("error", errorMessage);
        return errorJson.toString();
    }

    public static class SlideExpansion {
        public List<Slide> slides;

        public SlideExpansion(int size) {
            slides = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                slides.add(new Slide());
            }
        }

        public static class Slide {
            public String expandedContent;
        }
    }
}
