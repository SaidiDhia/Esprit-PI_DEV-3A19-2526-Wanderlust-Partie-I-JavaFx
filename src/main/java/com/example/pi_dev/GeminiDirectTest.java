package com.example.pi_dev;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GeminiDirectTest {
    public static void main(String[] args) {
        String apiKey = "AIzaSyDs0rgnlSxeM1gLuwRAcz4zil6umqabVVc";
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;

        String jsonBody = "{\"contents\":[{\"parts\":[{\"text\":\"Say hello in French\"}]}]}";

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            System.out.println("Sending request to: " + url);
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Status code: " + response.statusCode());
            System.out.println("Response body: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}