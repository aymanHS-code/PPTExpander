package com.powerpoint.expander;

import okhttp3.*;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.io.IOException;

public class ElevenLabsTTS {
    private static final String ELEVENLABS_API_KEY;
    private static final String ELEVENLABS_VOICE_ID;
    private static final String ELEVENLABS_API_URL;

    static {
        Dotenv dotenv = Dotenv.load();
        ELEVENLABS_API_KEY = dotenv.get("ELEVENLABS_API_KEY");
        ELEVENLABS_VOICE_ID = dotenv.get("ELEVENLABS_VOICE_ID");
        ELEVENLABS_API_URL = "https://api.elevenlabs.io/v1/text-to-speech/" + ELEVENLABS_VOICE_ID;
    }

    public static void generateSpeech(String text, String outputPath) throws IOException {
        OkHttpClient client = new OkHttpClient();

        JSONObject requestBody = new JSONObject();
        requestBody.put("text", text);
        requestBody.put("model_id", "eleven_multilingual_v2");
        requestBody.put("language_code", "en-US");

        JSONObject voiceSettings = new JSONObject();
        voiceSettings.put("stability", 0.5);
        voiceSettings.put("similarity_boost", 0.5);
        voiceSettings.put("style", 0);
        voiceSettings.put("use_speaker_boost", true);
        requestBody.put("voice_settings", voiceSettings);

        requestBody.put("apply_text_normalization", "auto");

        RequestBody body = RequestBody.create(requestBody.toString(), MediaType.parse("application/json"));

        Request request = new Request.Builder()
                .url(ELEVENLABS_API_URL)
                .post(body)
                .addHeader("accept", "audio/mpeg")
                .addHeader("xi-api-key", ELEVENLABS_API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        Response response = client.newCall(request).execute();

        if (response.isSuccessful() && response.body() != null) {
            byte[] audioBytes = response.body().bytes();
            java.nio.file.Files.write(new File(outputPath).toPath(), audioBytes);
        } else {
            throw new IOException("Failed to generate speech: " + response.code() + " " + response.message());
        }
    }
}
