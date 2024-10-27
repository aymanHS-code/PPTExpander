package com.powerpoint.expander;

import io.github.sashirestela.openai.SimpleOpenAI;
import io.github.sashirestela.openai.common.ResponseFormat;
import io.github.sashirestela.openai.common.ResponseFormat.JsonSchema;
import io.github.sashirestela.openai.domain.chat.*;
import io.github.sashirestela.openai.domain.chat.ChatMessage.SystemMessage;
import io.github.sashirestela.openai.domain.chat.ChatMessage.UserMessage;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartText;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl;
import io.github.sashirestela.openai.common.content.ContentPart.ContentPartImageUrl.ImageUrl;
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

    public static String expandSlideContents(List<SlideContent> slideContents, int maxTokens, String model) {
        LOGGER.info("Expanding slide contents. Number of slides: " + slideContents.size() + ", Max tokens: " + maxTokens + ", Model: " + model);
        
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.of(
            "As an AI professor, explain and expand on PowerPoint slide content for students in a manner to clarify the content given. " +
            "Follow these guidelines:\n\n" +
            "1. Present information in a clear, informative manner without directly addressing the listener.\n" +
            "2. Start explanations immediately with the content, avoiding phrases like 'This slide discusses...' or 'The slide titled...'\n" +
            "3. Explain concepts in detail, following the order presented in the original content.\n" +
            "4. Use and elaborate on examples provided to enhance understanding.\n" +
            "5. Present information in flowing paragraphs, using bullet points sparingly for lists or key points.\n" +
            "6. Maintain any structure of bullet points or numbered lists from the original content when necessary.\n" +
            "7. Provide additional context or examples only when it directly supports the content.\n" +
            "8. Break down complex ideas into simpler terms, always referring back to the original content.\n" +
            "9. Ensure the explanation is engaging and easy to follow, tailored for listening rather than reading.\n" +
            "10. Maintain a pace suitable for listening comprehension.\n" +
            "11. Do not introduce new topics or concepts not mentioned or implied in the original content.\n" +
            "12. Avoid phrases like 'in this lesson,' 'you will learn,' or directly addressing the listener as 'you.'\n" +
            "13. Focus on explaining the content objectively, as if providing information rather than teaching a lesson.\n" +
            "14. Do not use unreadable characters like emojis, symbols, or special characters (only use standard punctuation and numbers).\n\n" +
            "The primary focus is to explain and clarify the information presented, not to add extensive new information or frame it as a personal lesson."
        ));

        StringBuilder prompt = new StringBuilder("Expand on the following PowerPoint slide contents:\n\n");
        for (int i = 0; i < slideContents.size(); i++) {
            SlideContent slide = slideContents.get(i);
            prompt.append("Slide ").append(i + 1).append(":\n");
            
            if (slide.getText() != null) {
                prompt.append(slide.getText()).append("\n");
            }
            
            if (slide.getTable() != null) {
                prompt.append("Table content:\n").append(slide.getTable()).append("\n");
            }
            
            if (slide.getImageUrl() != null) {
                String imageDescription = describeImage(slide.getImageUrl());
                prompt.append("Image description: ").append(imageDescription).append("\n");
            }
            
            prompt.append("\n");
        }
        prompt.append("Format your response as a JSON object with an array of 'slides', each containing an 'expandedContent' field for each slide.");
        
        messages.add(UserMessage.of(prompt.toString()));

        LOGGER.info("Sending request to OpenAI API");
        ChatRequest chatRequest = ChatRequest.builder()
                .model(model)
                .messages(messages)
                .responseFormat(ResponseFormat.jsonSchema(JsonSchema.builder()
                .name("SlideExpansion")
                .schemaClass(SlideExpansion.class)
                .build()))
                .maxCompletionTokens(maxTokens)
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

    private static String describeImage(String imageUrl) {
        var chatRequest = ChatRequest.builder()
                .model("gpt-4o-mini")
                .messages(List.of(
                        UserMessage.of(List.of(
                            ContentPartText.of("What do you see in the image? Give in details in no more than 100 words."),
                                ContentPartImageUrl.of(ImageUrl.of(imageUrl))))))
                .temperature(0.0)
                .maxCompletionTokens(1000)
                .build();
        var chatResponse = openAI.chatCompletions().create(chatRequest).join();
        return chatResponse.firstContent();
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
