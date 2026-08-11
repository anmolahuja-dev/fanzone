package com.fanzone.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Integration test: Post flow.
 * create post → appears in feed → comment → upvote → reputation update (Kafka)
 *
 * Requires Docker + all services running.
 */
@Disabled("Requires Docker Compose infrastructure — run manually")
class PostFlowIntegrationTest {

    // TODO: Implement with Testcontainers
    // 1. Login to get auth token
    // 2. POST /api/v1/posts (text post) → 201
    // 3. GET /api/v1/feeds/club → verify post appears
    // 4. POST /api/v1/posts/{id}/comments → 201
    // 5. POST /api/v1/posts/{id}/upvotes → 200
    // 6. Verify reputation-service consumed PostUpvotedEvent (check user reputation)

    @Test
    void createPostAndInteract() {
        // Placeholder
    }
}
