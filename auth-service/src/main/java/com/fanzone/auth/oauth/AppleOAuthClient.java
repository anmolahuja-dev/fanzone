package com.fanzone.auth.oauth;

import com.fanzone.auth.dto.OAuthUserInfo;
import com.fanzone.common.exceptions.AuthenticationException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AppleOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(AppleOAuthClient.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final Duration KEY_CACHE_DURATION = Duration.ofHours(24);

    private final WebClient webClient;
    private final String keyUrl;

    private final ConcurrentHashMap<String, PublicKey> cachedKeys = new ConcurrentHashMap<>();
    private volatile Instant lastKeyFetchTime = Instant.EPOCH;

    public AppleOAuthClient(
            WebClient.Builder webClientBuilder,
            @Value("${fanzone.oauth.apple.key-url}") String keyUrl) {
        this.webClient = webClientBuilder.build();
        this.keyUrl = keyUrl;
    }

    public OAuthUserInfo verifyToken(String idToken) {
        try {
            refreshKeysIfNeeded();

            Claims claims = parseAndVerifyToken(idToken);

            String email = claims.get("email", String.class);
            if (email == null || email.isBlank()) {
                throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
            }

            String name = claims.get("name", String.class);
            String displayName = (name != null && !name.isBlank()) ? name : email.split("@")[0];

            return new OAuthUserInfo(email, displayName, "apple");

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple OAuth token verification failed", e);
            throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
        }
    }

    private Claims parseAndVerifyToken(String idToken) {
        String header = idToken.split("\\.")[0];
        String decodedHeader = new String(Base64.getUrlDecoder().decode(header));
        String kid = extractKid(decodedHeader);

        PublicKey publicKey = cachedKeys.get(kid);
        if (publicKey == null) {
            // Force refresh and retry
            forceRefreshKeys();
            publicKey = cachedKeys.get(kid);
            if (publicKey == null) {
                throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
            }
        }

        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(idToken)
                .getPayload();
    }

    private String extractKid(String headerJson) {
        // Simple extraction of "kid" from JSON header without external JSON parser
        int kidIdx = headerJson.indexOf("\"kid\"");
        if (kidIdx == -1) {
            throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
        }
        int colonIdx = headerJson.indexOf(":", kidIdx);
        int firstQuote = headerJson.indexOf("\"", colonIdx + 1);
        int secondQuote = headerJson.indexOf("\"", firstQuote + 1);
        return headerJson.substring(firstQuote + 1, secondQuote);
    }

    private void refreshKeysIfNeeded() {
        if (Duration.between(lastKeyFetchTime, Instant.now()).compareTo(KEY_CACHE_DURATION) > 0) {
            forceRefreshKeys();
        }
    }

    private synchronized void forceRefreshKeys() {
        // Double-check after acquiring lock
        if (Duration.between(lastKeyFetchTime, Instant.now()).compareTo(Duration.ofMinutes(1)) < 0
                && !cachedKeys.isEmpty()) {
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(keyUrl)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null || !response.containsKey("keys")) {
                log.error("Failed to fetch Apple public keys: empty response");
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, String>> keys = (List<Map<String, String>>) response.get("keys");
            ConcurrentHashMap<String, PublicKey> newKeys = new ConcurrentHashMap<>();

            for (Map<String, String> key : keys) {
                String kid = key.get("kid");
                String n = key.get("n");
                String e = key.get("e");

                if (kid != null && n != null && e != null) {
                    PublicKey publicKey = buildRsaPublicKey(n, e);
                    newKeys.put(kid, publicKey);
                }
            }

            cachedKeys.clear();
            cachedKeys.putAll(newKeys);
            lastKeyFetchTime = Instant.now();

            log.info("Successfully refreshed Apple public keys. {} keys cached.", newKeys.size());

        } catch (Exception e) {
            log.error("Failed to refresh Apple public keys", e);
        }
    }

    private PublicKey buildRsaPublicKey(String modulusBase64, String exponentBase64) throws Exception {
        byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
        byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);

        BigInteger modulus = new BigInteger(1, modulusBytes);
        BigInteger exponent = new BigInteger(1, exponentBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePublic(spec);
    }
}
