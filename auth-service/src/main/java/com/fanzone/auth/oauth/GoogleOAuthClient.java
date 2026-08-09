package com.fanzone.auth.oauth;

import com.fanzone.auth.dto.OAuthUserInfo;
import com.fanzone.common.exceptions.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Map;

@Component
public class GoogleOAuthClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthClient.class);
    private static final String TOKEN_INFO_URL = "https://oauth2.googleapis.com/tokeninfo";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;
    private final String clientId;

    public GoogleOAuthClient(
            WebClient.Builder webClientBuilder,
            @Value("${fanzone.oauth.google.client-id}") String clientId) {
        this.webClient = webClientBuilder.baseUrl(TOKEN_INFO_URL).build();
        this.clientId = clientId;
    }

    public OAuthUserInfo verifyToken(String idToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("id_token", idToken)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(TIMEOUT)
                    .block();

            if (response == null) {
                throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
            }

            String email = (String) response.get("email");
            String name = (String) response.get("name");
            String aud = (String) response.get("aud");

            if (email == null || email.isBlank()) {
                throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
            }

            if (!clientId.equals(aud)) {
                log.warn("Google token audience mismatch. Expected: {}, Got: {}", clientId, aud);
                throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
            }

            String displayName = (name != null && !name.isBlank()) ? name : email.split("@")[0];

            return new OAuthUserInfo(email, displayName, "google");

        } catch (AuthenticationException e) {
            throw e;
        } catch (WebClientResponseException e) {
            log.error("Google OAuth token verification failed with status: {}", e.getStatusCode(), e);
            throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
        } catch (Exception e) {
            log.error("Google OAuth token verification failed", e);
            throw new AuthenticationException("AUTH_OAUTH_FAILED", "OAuth authentication failed");
        }
    }
}
