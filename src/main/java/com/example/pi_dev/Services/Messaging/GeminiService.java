package com.example.pi_dev.Services.Messaging;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiService {
    // Your API key (works fine)
    private static final String API_KEY = "AIzaSyDYNBZev4Y1CK--PTFzVY0cuhzvurxeEQM";

    // FIXED: Correct API endpoint with proper model name
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=" + API_KEY;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String generateResponse(String prompt) {
        try {
            System.out.println("📤 Sending to Gemini: " + prompt);

            String requestBody = createRequestBody(prompt);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("📥 Response status: " + response.statusCode());
            System.out.println("📥 Response body: " + response.body());

            if (response.statusCode() == 200) {
                return parseResponse(response.body());
            } else {
                return "Error: API returned status " + response.statusCode() + " - " + response.body();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error calling Gemini API: " + e.getMessage();
        }
    }

    private String createRequestBody(String prompt) {
        JsonObject requestBody = new JsonObject();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();

        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);

        JsonArray contents = new JsonArray();
        contents.add(content);
        requestBody.add("contents", contents);

        return requestBody.toString();
    }

    private String parseResponse(String jsonResponse) {
        try {
            JsonObject response = JsonParser.parseString(jsonResponse).getAsJsonObject();

            JsonArray candidates = response.getAsJsonArray("candidates");
            if (candidates != null && candidates.size() > 0) {
                JsonObject firstCandidate = candidates.get(0).getAsJsonObject();
                JsonObject content = firstCandidate.getAsJsonObject("content");
                JsonArray parts = content.getAsJsonArray("parts");
                if (parts != null && parts.size() > 0) {
                    JsonObject firstPart = parts.get(0).getAsJsonObject();
                    return firstPart.get("text").getAsString();
                }
            }
            return "No response generated";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error parsing response: " + e.getMessage();
        }
    }
    //hethy ena ntasti beha
    public static void main(String[] args) {
        GeminiService gemini = new GeminiService();
        String response = gemini.generateResponse("Say hello in French");
        System.out.println("✅ Final response: " + response);
    }
}