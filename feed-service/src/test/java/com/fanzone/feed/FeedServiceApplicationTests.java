package com.fanzone.feed;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic smoke test for feed-service.
 * Full context load test requires Kafka + Redis infrastructure.
 * Use Docker Compose integration tests for full validation.
 */
class FeedServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertNotNull(FeedServiceApplication.class);
    }

}
