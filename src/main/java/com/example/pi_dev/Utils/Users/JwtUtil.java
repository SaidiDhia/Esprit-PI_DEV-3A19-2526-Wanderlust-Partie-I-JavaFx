package com.example.pi_dev.Utils.Users;

import com.example.pi_dev.Entities.Users.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JwtUtil {

    // Use a strong secret key. In production, store this in environment variables.
    private static final Key SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_TIME = 86400000; // 24 hours

    public static String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());
        claims.put("email", user.getEmail());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUserId().toString())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validateToken(String token, User user) {
        final String userId = extractUserId(token);
        return (userId.equals(user.getUserId().toString()) && !isTokenExpired(token));
    }

    public static String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    public static Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static boolean isTokenExpired(String token) {
        return extractClaims(token).getExpiration().before(new Date());
    }
}
