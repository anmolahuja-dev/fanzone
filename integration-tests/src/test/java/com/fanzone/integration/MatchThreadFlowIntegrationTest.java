package com.fanzone.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test: Match Thread flow.
 * join → live comments (WebSocket) → player ratings → MOTM display
 *
 * Requires Docker + all services running.
 */
@Disabled("Requires Docker Compose infrastructure — run manually")
class MatchThreadFlowIntegrationTest {

    // TODO: Implement with Testcontainers + WebSocket client
    // 1. Login to get auth token
    // 2. GET /api/v1/matches → list matches
    // 3. Connect WebSocket to /ws, subscribe to /topic/match-threads/{matchId}
    // 4. POST /api/v1/match-threads/{matchId}/comments → verify broadcast
    // 5. POST /api/v1/match-threads/{matchId}/ratings → submit ratings
    // 6. GET /api/v1/match-threads/{matchId}/motm → verify MOTM

    @Test
    void liveMatchThread() {
        // Placeholder
    }
}
