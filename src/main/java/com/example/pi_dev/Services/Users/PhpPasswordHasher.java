package com.example.pi_dev.Services.Users;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PhpPasswordHasher {

    private static final int DEFAULT_COST = 13;

    private PhpPasswordHasher() {
    }

    static String hashBcrypt(String password) {
        return hashBcrypt(password, DEFAULT_COST);
    }

    static String hashBcrypt(String password, int cost) {
        String result = runPhp("echo password_hash($argv[1], PASSWORD_BCRYPT, ['cost' => (int)$argv[2]]);", password,
                String.valueOf(cost));
        if (result == null || result.isBlank()) {
            throw new IllegalStateException("Unable to hash password using PHP CLI");
        }
        return result;
    }

    static boolean verifyBcrypt(String password, String hash) {
        if (password == null || hash == null || hash.isBlank()) {
            return false;
        }

        String result = runPhp("echo password_verify($argv[1], $argv[2]) ? '1' : '0';", password, hash);
        return "1".equals(result);
    }

    private static String runPhp(String code, String... arguments) {
        List<String> command = new ArrayList<>();
        command.add("php");
        command.add("-r");
        command.add(code);
        for (String argument : arguments) {
            command.add(argument);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return null;
            }

            return output.toString().trim();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
}