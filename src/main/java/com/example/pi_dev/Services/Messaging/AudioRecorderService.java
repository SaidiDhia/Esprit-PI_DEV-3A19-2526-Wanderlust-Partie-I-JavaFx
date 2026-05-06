package com.example.pi_dev.Services.Messaging;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AudioRecorderService {

    private static final String AUDIO_DIR = "uploads/audio/";
    private TargetDataLine targetDataLine;
    private boolean isRecording = false;
    private ByteArrayOutputStream byteArrayOutputStream;
    private Thread recordingThread;

    public AudioRecorderService() {
        new File(AUDIO_DIR).mkdirs();
    }

    /**
     * Start recording audio
     */
    public void startRecording() throws LineUnavailableException {
        if (isRecording) return;

        // Audio format: 44.1kHz, 16-bit, stereo
        AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio recording not supported");
        }

        targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
        targetDataLine.open(format);
        targetDataLine.start();

        byteArrayOutputStream = new ByteArrayOutputStream();
        isRecording = true;

        recordingThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            try {
                while (isRecording) {
                    int bytesRead = targetDataLine.read(buffer, 0, buffer.length);
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        recordingThread.start();
    }

    /**
     * Stop recording and save to file
     */
    public File stopRecording() throws IOException {
        if (!isRecording) return null;

        isRecording = false;
        targetDataLine.stop();
        targetDataLine.close();

        try {
            recordingThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Generate filename with timestamp
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "voice_" + timestamp + ".wav";
        Path filePath = Paths.get(AUDIO_DIR + fileName);

        // Save audio data to file
        byte[] audioData = byteArrayOutputStream.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(audioData);
        AudioInputStream audioInputStream = new AudioInputStream(
                bais,
                targetDataLine.getFormat(),
                audioData.length / targetDataLine.getFormat().getFrameSize()
        );

        File outputFile = filePath.toFile();
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, outputFile);

        return outputFile;
    }

    /**
     * Cancel recording without saving
     */
    public void cancelRecording() {
        isRecording = false;
        if (targetDataLine != null) {
            targetDataLine.stop();
            targetDataLine.close();
        }
    }

    /**
     * Get audio duration in seconds
     */
    public static int getAudioDuration(File audioFile) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioInputStream.getFormat();
            long frames = audioInputStream.getFrameLength();
            return (int) (frames / format.getFrameRate());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}