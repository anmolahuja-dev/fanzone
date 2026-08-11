package com.fanzone.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test: Auth flow.
 * register → verify email → login → JWT token → access protected endpoint
 *
 * Requires Docker + all services running via Docker Compose.
 */
@Disabled("Requires Docker Compose infrastructure — run manually")
class AuthFlowIntegrationTest {

    // TODO: Implement with Testcontainers + WebTestClient
    // 1. Start PostgreSQL, Redis, Kafka via Testcontainers
    // 2. Start auth-service Spring Boot app
    // 3. POST /api/v1/auth/register → 201, get tokens
    // 4. POST /api/v1/auth/verify-email → 200
    // 5. POST /api/v1/auth/login → 200, get access token
    // 6. GET /api/v1/users/me (with token) → 200

    @Test
    void registerAndLogin() {
        // Placeholder for full integration test
    }
}
