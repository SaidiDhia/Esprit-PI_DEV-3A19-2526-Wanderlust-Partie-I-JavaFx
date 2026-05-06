package com.example.pi_dev.Services.Messaging;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import java.io.*;

public class SimpleTTSService {

    private Process currentProcess;
    private boolean isSupported = true;
    private String os;

    public SimpleTTSService() {
        os = System.getProperty("os.name").toLowerCase();
        checkSupport();
    }

    private void checkSupport() {
        if (os.contains("windows")) {
            System.out.println("✅ Windows TTS supported - using PowerShell SpeechSynthesizer");
            isSupported = true;
        } else if (os.contains("mac")) {
            System.out.println("✅ Mac TTS supported - using 'say' command");
            isSupported = true;
        } else if (os.contains("linux")) {
            System.out.println("⚠️ Linux TTS supported (requires espeak)");
            isSupported = true;
        } else {
            isSupported = false;
            System.out.println("❌ TTS not supported on this OS: " + os);
        }
    }

    /**
     * Speak a message using system TTS
     */
    public void speakMessage(String message) {
        if (!isSupported) {
            showAlert("TTS not supported on your operating system");
            return;
        }

        if (message == null || message.trim().isEmpty()) {
            System.err.println("Empty message - nothing to speak");
            return;
        }

        // Stop any currently playing speech
        stopSpeaking();

        new Thread(() -> {
            try {
                if (os.contains("windows")) {
                    speakWindows(message);
                } else if (os.contains("mac")) {
                    speakMac(message);
                } else if (os.contains("linux")) {
                    speakLinux(message);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> showAlert("Failed to speak: " + e.getMessage()));
            }
        }).start();
    }

    /**
     * Windows implementation using PowerShell SpeechSynthesizer
     */
    private void speakWindows(String message) throws IOException {
        // Escape single quotes for PowerShell
        String escapedMessage = message.replace("'", "''");

        // PowerShell command for TTS with better error handling
        String psCommand =
                "$ErrorActionPreference = 'Stop'; " +
                        "try { " +
                        "   Add-Type -AssemblyName System.Speech; " +
                        "   $synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                        "   $synth.Rate = 0; " +  // Normal speed
                        "   $synth.Volume = 100; " +  // Max volume
                        "   $synth.Speak('" + escapedMessage + "'); " +
                        "} catch { " +
                        "   Write-Error $_.Exception.Message; " +
                        "   exit 1; " +
                        "}";

        ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", psCommand);
        pb.redirectErrorStream(true);
        currentProcess = pb.start();

        // Log output for debugging
        BufferedReader reader = new BufferedReader(new InputStreamReader(currentProcess.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("PowerShell TTS: " + line);
        }

        System.out.println("🔊 Speaking: \"" + message + "\"");
    }

    /**
     * Mac implementation using 'say' command
     */
    private void speakMac(String message) throws IOException {
        // Use different voices on Mac
        String[] voices = { "Daniel", "Samantha", "Alex" };
        String selectedVoice = voices[0]; // Default to Daniel

        String[] command = { "say", "-v", selectedVoice, message };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        currentProcess = pb.start();

        System.out.println("🔊 Speaking on Mac: \"" + message + "\"");
    }

    /**
     * Linux implementation using 'espeak' command
     */
    private void speakLinux(String message) throws IOException {
        // Check if espeak is installed
        Process checkProcess = Runtime.getRuntime().exec(new String[]{"which", "espeak"});
        try {
            if (checkProcess.waitFor() != 0) {
                Platform.runLater(() -> showAlert(
                        "espeak not installed. Run: sudo apt-get install espeak"));
                return;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        String[] command = { "espeak", "-s", "150", "-p", "50", message };
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        currentProcess = pb.start();

        System.out.println("🔊 Speaking on Linux: \"" + message + "\"");
    }

    /**
     * Stop any ongoing speech
     */
    public void stopSpeaking() {
        if (currentProcess != null && currentProcess.isAlive()) {
            currentProcess.destroy();
            currentProcess = null;
            System.out.println("⏹️ Speech stopped");
        }
    }

    /**
     * Change voice (Windows only)
     */
    public void setVoice(String voiceName) {
        if (!os.contains("windows")) {
            System.out.println("Voice selection only supported on Windows");
            return;
        }

        // This just demonstrates - actual voice change would need to be set in the PowerShell command
        System.out.println("Voice would be set to: " + voiceName);
    }

    /**
     * List available Windows voices
     */
    public void listWindowsVoices() {
        if (!os.contains("windows")) {
            System.out.println("Voice listing only supported on Windows");
            return;
        }

        try {
            String psCommand =
                    "Add-Type -AssemblyName System.Speech; " +
                            "$synth = New-Object System.Speech.Synthesis.SpeechSynthesizer; " +
                            "$synth.GetInstalledVoices() | ForEach-Object { $_.VoiceInfo.Name }";

            Process process = Runtime.getRuntime().exec(new String[]{"powershell.exe", "-Command", psCommand});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            System.out.println("📢 Available Windows voices:");
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    System.out.println("  - " + line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if service is available
     */
    public boolean isAvailable() {
        return isSupported;
    }

    private void showAlert(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("TTS Warning");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }
}