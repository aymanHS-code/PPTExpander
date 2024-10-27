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
import java.util.Optional;
import io.github.cdimascio.dotenv.Dotenv;

public class OpenAIExpander {
    private static final Logger LOGGER = Logger.getLogger(OpenAIExpander.class.getName());
    private static final String API_KEY;

    static {
        Dotenv dotenv = Dotenv.load();
        API_KEY = Optional.ofNullable(dotenv.get("OPENAI_API_KEY"))
                .orElseThrow(() -> new IllegalStateException("OPENAI_API_KEY is not set in the .env file"));
    }
    private static final SimpleOpenAI openAI = SimpleOpenAI.builder().apiKey(API_KEY).build();

    public static String expandSlideContents(List<String> slideContents) {
        LOGGER.info("Expanding slide contents. Number of slides: " + slideContents.size());
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.of("You are an AI tutor designed to explain and expand on PowerPoint slide content directly to students. Your goal is to help students understand the material better. Speak in a friendly, first-person manner as if you're talking directly to the student(s). Explain concepts clearly, use relatable examples, and break down complex ideas. Your explanations should be engaging, easy to follow, and tailored for listening. Avoid mentioning that you're expanding on slides; instead, naturally incorporate the slide's content into your explanation. Make sure your pace is suitable for listening, neither too fast nor too slow."));

        StringBuilder prompt = new StringBuilder("Expand on the following PowerPoint slide contents:\n\n");
        for (int i = 0; i < slideContents.size(); i++) {
            prompt.append("Slide ").append(i + 1).append(":\n").append(slideContents.get(i)).append("\n\n");
        }
        prompt.append("Format your response as a JSON object with an array of 'slides', each containing an 'expandedContent' field for each slide.");
        
        messages.add(UserMessage.of(prompt.toString()));

        LOGGER.info("Sending request to OpenAI API");
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
                LOGGER.info("Parsed JSON response: " + jsonResponse.toString(2));
                
                if (!jsonResponse.has("slides")) {
                    LOGGER.info("Response doesn't have 'slides' key. Wrapping content.");
                    // If the response doesn't have a "slides" key, wrap it in one
                    JSONObject wrappedResponse = new JSONObject();
                    JSONArray slidesArray = new JSONArray();
                    for (int i = 0; i < slideContents.size(); i++) {
                        String key = "slide" + (i + 1);
                        if (jsonResponse.has(key)) {
                            JSONObject slideObject = new JSONObject();
                            slideObject.put("expandedContent", jsonResponse.getString(key));
                            slidesArray.put(slideObject);
                        }
                    }
                    wrappedResponse.put("slides", slidesArray);
                    LOGGER.info("Wrapped response: " + wrappedResponse.toString(2));
                    return wrappedResponse.toString();
                }
                LOGGER.info("Returning original JSON response");
                return jsonResponse.toString();
            } catch (JSONException e) {
                LOGGER.warning("Failed to parse JSON response: " + e.getMessage());
                return createErrorJson("Invalid JSON response from API: " + e.getMessage());
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
