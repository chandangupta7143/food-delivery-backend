package com.fooddelivery.surge.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.logging.Logger;

@Service
public class HmacTokenService {

    private static final Logger LOGGER = Logger.getLogger(HmacTokenService.class.getName());
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secretKey;
    private final org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    public HmacTokenService(
            @Value("${surge.token.secret:default-surge-token-shared-secret-key-32-bytes-long}") String secretKey,
            org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate) {
        this.secretKey = secretKey;
        this.redisTemplate = redisTemplate;
    }

    public String generateToken(String userId, String restaurantId, String h3Index, double surgeMultiplier, double deliveryFee, long expiresAt) {
        String nonce = java.util.UUID.randomUUID().toString();
        String payload = buildPayload(userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, expiresAt, nonce);
        String signature = sign(payload, secretKey);
        
        // Return a combined token: Base64(payload) + "." + signature
        String base64Payload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return base64Payload + "." + signature;
    }

    public boolean verifyToken(String token, String userId, String restaurantId, String h3Index, double surgeMultiplier, double deliveryFee) {
        if (token == null || !token.contains(".")) {
            return false;
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                return false;
            }

            String base64Payload = parts[0];
            String signature = parts[1];

            String decodedPayload = new String(Base64.getUrlDecoder().decode(base64Payload), StandardCharsets.UTF_8);
            
            // Expected payload parameters parsing (supports legacy 6-field and secure 7-field nonce payloads)
            String[] fields = decodedPayload.split(":");
            if (fields.length < 6 || fields.length > 7) {
                return false;
            }

            String tokenUserId = fields[0];
            String tokenRestaurantId = fields[1];
            String tokenH3Index = fields[2];
            double tokenMultiplier = Double.parseDouble(fields[3]);
            double tokenFee = Double.parseDouble(fields[4]);
            long tokenExpiresAt = Long.parseLong(fields[5]);
            String nonce = fields.length >= 7 ? fields[6] : null;

            // 1. Check expiration (tokenExpiresAt is epoch seconds)
            long currentEpochSec = System.currentTimeMillis() / 1000;
            if (currentEpochSec > tokenExpiresAt) {
                LOGGER.warning("Surge quote token has expired");
                return false;
            }

            // 2. Replay Attack Verification (nonce check)
            if (nonce != null) {
                String replayKey = "token:used:" + nonce;
                if (Boolean.TRUE.equals(redisTemplate.hasKey(replayKey))) {
                    LOGGER.warning("Replay attack detected for token nonce: " + nonce);
                    return false;
                }
                long remainingTtl = tokenExpiresAt - currentEpochSec;
                if (remainingTtl > 0) {
                    redisTemplate.opsForValue().set(replayKey, "USED", java.time.Duration.ofSeconds(remainingTtl));
                }
            }

            // 3. Verify all matching inputs to prevent hijacking
            if (!tokenUserId.equals(userId) ||
                !tokenRestaurantId.equals(restaurantId) ||
                !tokenH3Index.equalsIgnoreCase(h3Index) ||
                Math.abs(tokenMultiplier - surgeMultiplier) > 0.001 ||
                Math.abs(tokenFee - deliveryFee) > 0.001) {
                LOGGER.warning("Token payload parameters do not match requested checkout parameters");
                return false;
            }

            // 4. Verify signature integrity
            String expectedPayload = fields.length >= 7 
                ? buildPayload(userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, tokenExpiresAt, nonce)
                : String.format("%s:%s:%s:%.2f:%.2f:%d", userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, tokenExpiresAt);
            String expectedSignature = sign(expectedPayload, secretKey);

            return expectedSignature.equals(signature);
        } catch (Exception e) {
            LOGGER.severe("Exception occurred during surge token verification: " + e.getMessage());
            return false;
        }
    }

    private String buildPayload(String userId, String restaurantId, String h3Index, double surgeMultiplier, double deliveryFee, long expiresAt, String nonce) {
        // userId:restaurantId:h3Index:surgeMultiplier:deliveryFee:expiresAt:nonce
        return String.format("%s:%s:%s:%.2f:%.2f:%d:%s", userId, restaurantId, h3Index, surgeMultiplier, deliveryFee, expiresAt, nonce);
    }

    private String sign(String data, String secret) {
        try {
            byte[] hash = secret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(hash, HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] signedBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signedBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error signing HMAC payload", e);
        }
    }
}
