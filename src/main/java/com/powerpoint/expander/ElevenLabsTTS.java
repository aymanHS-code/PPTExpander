package com.powerpoint.expander;

import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import io.github.cdimascio.dotenv.Dotenv;

public class ElevenLabsTTS {
    private static final Logger LOGGER = Logger.getLogger(ElevenLabsTTS.class.getName());
    private static final String API_KEY;
    private static final String VOICE_ID;
    private static final int MAX_RETRIES = 3;
    private static final int TIMEOUT_SECONDS = 60;

    static {
        Dotenv dotenv = Dotenv.load();
        API_KEY = dotenv.get("ELEVENLABS_API_KEY");
        VOICE_ID = dotenv.get("ELEVENLABS_VOICE_ID");
    }

    public static void generateSpeech(String text, String outputPath) throws IOException {
        LOGGER.info("Generating speech for text: " + text.substring(0, Math.min(text.length(), 50)) + "...");
        LOGGER.info("Output path: " + outputPath);

        OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();

        MediaType mediaType = MediaType.parse("application/json");
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", text);
        requestBody.put("model_id", "eleven_multilingual_v2");
        requestBody.put("voice_settings", new JSONObject().put("stability", 0.5).put("similarity_boost", 0.5));

        RequestBody body = RequestBody.create(requestBody.toString(), mediaType);
        Request request = new Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/" + VOICE_ID)
                .post(body)
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .addHeader("xi-api-key", API_KEY)
                .build();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    LOGGER.warning("Failed to generate speech. Response code: " + response.code());
                    LOGGER.warning("Response body: " + response.body().string());
                    if (attempt == MAX_RETRIES) {
                        throw new IOException("Failed to generate speech after " + MAX_RETRIES + " attempts: " + response.code() + " " + response.message());
                    }
                    LOGGER.info("Retrying... (Attempt " + (attempt + 1) + " of " + MAX_RETRIES + ")");
                    continue;
                }

                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    Files.write(Paths.get(outputPath), responseBody.bytes());
                    LOGGER.info("Speech generated successfully and saved to: " + outputPath);
                    return;
                } else {
                    LOGGER.severe("Response body is null");
                    throw new IOException("Failed to generate speech: Response body is null");
                }
            } catch (IOException e) {
                if (attempt == MAX_RETRIES) {
                    throw e;
                }
                LOGGER.warning("Attempt " + attempt + " failed: " + e.getMessage());
                LOGGER.info("Retrying... (Attempt " + (attempt + 1) + " of " + MAX_RETRIES + ")");
            }
        }
    }
}
