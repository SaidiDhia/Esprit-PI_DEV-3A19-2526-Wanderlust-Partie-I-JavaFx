package com.example.pi_dev.Services.Events;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class WeatherService {

    private static final String API_KEY = "79f2e054a0d8ea223d4c50292e3c1f1c";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    private static final Map<String, Map<String, String>> RISK_ACTIVITIES = new HashMap<>();

    static {
        Map<String, String> rainRisks = new HashMap<>();
        rainRisks.put("MER", "Plongée sous-marine");
        rainRisks.put("AERIEN", "Parapente");
        rainRisks.put("NATURE", "Randonnée en montagne");
        RISK_ACTIVITIES.put("rain", rainRisks);

        Map<String, String> snowRisks = new HashMap<>();
        snowRisks.put("MER", "Bateau");
        snowRisks.put("AERIEN", "Vol en montgolfière");
        snowRisks.put("NATURE", "Camping");
        RISK_ACTIVITIES.put("snow", snowRisks);

        Map<String, String> stormRisks = new HashMap<>();
        stormRisks.put("MER", "Toutes activités nautiques");
        stormRisks.put("AERIEN", "Toutes activités aériennes");
        stormRisks.put("NATURE", "Randonnée");
        stormRisks.put("DESERT", "Safari");
        RISK_ACTIVITIES.put("thunderstorm", stormRisks);

        Map<String, String> extremeHeatRisks = new HashMap<>();
        extremeHeatRisks.put("DESERT", "Trekking");
        extremeHeatRisks.put("NATURE", "Randonnée longue");
        extremeHeatRisks.put("MER", "Plongée");
        RISK_ACTIVITIES.put("extreme_heat", extremeHeatRisks);
    }

    public interface WeatherCallback {
        void onWeatherReceived(WeatherData weather);
        void onError(String error);
    }

    public static class WeatherData {
        private String condition;
        private double temperature;
        private double windSpeed;
        private int humidity;
        private String description;
        private String cityName;
        private boolean isDangerous;
        private String riskMessage;

        public WeatherData(String condition, double temperature, double windSpeed, int humidity,
                           String description, String cityName) {
            this.condition = condition;
            this.temperature = temperature;
            this.windSpeed = windSpeed;
            this.humidity = humidity;
            this.description = description;
            this.cityName = cityName;
            this.isDangerous = isDangerousCondition(condition);
            this.riskMessage = generateRiskMessage(condition);
        }

        private boolean isDangerousCondition(String condition) {
            return condition.contains("rain") || condition.contains("storm") ||
                    condition.contains("snow") || condition.contains("extreme_heat");
        }

        private String generateRiskMessage(String condition) {
            StringBuilder message = new StringBuilder();
            Map<String, String> risks = RISK_ACTIVITIES.get(condition);
            if (risks != null) {
                message.append("⚠️ Conditions météo défavorables:\n");
                for (Map.Entry<String, String> entry : risks.entrySet()) {
                    message.append("• ").append(entry.getValue()).append(" (").append(entry.getKey()).append(")\n");
                }
            }
            return message.toString();
        }

        public String getCondition() { return condition; }
        public double getTemperature() { return temperature; }
        public double getWindSpeed() { return windSpeed; }
        public int getHumidity() { return humidity; }
        public String getDescription() { return description; }
        public String getCityName() { return cityName; }
        public boolean isDangerous() { return isDangerous; }
        public String getRiskMessage() { return riskMessage; }

        public String getTemperatureDisplay() {
            return String.format("%.1f°C", temperature);
        }

        public String getWindSpeedDisplay() {
            return String.format("%.1f km/h", windSpeed * 3.6);
        }
    }

    public void getCurrentWeather(String cityName, WeatherCallback callback) {
        new Thread(() -> {
            try {
                String urlString = String.format("%s?q=%s&appid=%s&units=metric&lang=fr",
                        BASE_URL, cityName, API_KEY);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                WeatherData weather = parseWeatherResponse(response.toString());
                Platform.runLater(() -> callback.onWeatherReceived(weather));

            } catch (Exception e) {
                Platform.runLater(() -> callback.onError("Erreur lors de la récupération des données météo: " + e.getMessage()));
            }
        }).start();
    }

    private WeatherData parseWeatherResponse(String jsonResponse) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);

        String cityName = root.path("name").asText();
        double temperature = root.path("main").path("temp").asDouble();
        int humidity = root.path("main").path("humidity").asInt();
        double windSpeed = root.path("wind").path("speed").asDouble();

        JsonNode weather = root.path("weather").get(0);
        String mainCondition = weather.path("main").asText().toLowerCase();
        String description = weather.path("description").asText();

        String condition = determineWeatherCondition(mainCondition, description, temperature);

        return new WeatherData(condition, temperature, windSpeed, humidity, description, cityName);
    }

    private String determineWeatherCondition(String main, String description, double temperature) {
        if (main.contains("rain") || main.contains("drizzle")) {
            return "rain";
        } else if (main.contains("snow")) {
            return "snow";
        } else if (main.contains("thunderstorm")) {
            return "thunderstorm";
        } else if (temperature > 40) {
            return "extreme_heat";
        } else {
            return "clear";
        }
    }

    public VBox createWeatherWidget(WeatherData weather) {
        VBox weatherBox = new VBox(10);
        weatherBox.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; -fx-border-color: #E2E8F0; -fx-border-width: 1;");

        Label weatherTitle = new Label("🌤️ Météo Actuelle");
        weatherTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0F2C4F;");

        HBox tempBox = new HBox(15);
        Label cityLabel = new Label(weather.getCityName());
        cityLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #2D70B3;");

        Label tempLabel = new Label(weather.getTemperatureDisplay());
        tempLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0F2C4F;");

        tempBox.getChildren().addAll(cityLabel, tempLabel);

        Label descLabel = new Label(weather.getDescription());
        descLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #64748B; -fx-font-style: italic;");

        HBox detailsBox = new HBox(20);
        Label windLabel = new Label("💨 " + weather.getWindSpeedDisplay());
        windLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        Label humidityLabel = new Label("💧 " + weather.getHumidity() + "%");
        humidityLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748B;");

        detailsBox.getChildren().addAll(windLabel, humidityLabel);

        if (weather.isDangerous()) {
            Label alertLabel = new Label("⚠️ " + weather.getRiskMessage());
            alertLabel.setStyle("-fx-background-color: #FEF3C7; -fx-text-fill: #92400E; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 12px;");
            alertLabel.setWrapText(true);

            weatherBox.getChildren().addAll(weatherTitle, tempBox, descLabel, detailsBox, alertLabel);
        } else {
            Label safeLabel = new Label("✅ Conditions météo favorables pour les activités");
            safeLabel.setStyle("-fx-background-color: #D1FAE5; -fx-text-fill: #065F46; -fx-background-radius: 8; -fx-padding: 10; -fx-font-size: 12px;");

            weatherBox.getChildren().addAll(weatherTitle, tempBox, descLabel, detailsBox, safeLabel);
        }

        return weatherBox;
    }

    public void showWeatherAlert(WeatherData weather, String activityCategory) {
        if (!weather.isDangerous()) return;

        Map<String, String> risks = RISK_ACTIVITIES.get(weather.getCondition());
        if (risks != null && risks.containsKey(activityCategory)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("⚠️ Alerte Météo");
            alert.setHeaderText("Conditions météo défavorables pour cette activité");

            String content = "Météo actuelle: " + weather.getDescription() + "\n" +
                    "Température: " + weather.getTemperatureDisplay() + "\n" +
                    "Activité à risque: " + risks.get(activityCategory) + "\n\n" +
                    "⚠️ Il est déconseillé de pratiquer cette activité dans ces conditions.\n" +
                    "Veuillez reporter votre activité ou choisir une autre activité plus adaptée.";

            alert.setContentText(content);
            alert.showAndWait();
        }
    }
}