package com.fanzone.post;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic smoke test for post-service.
 * Full context load test requires Kafka + Redis + PostgreSQL infrastructure.
 * Use Docker Compose integration tests for full validation.
 */
class PostServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertNotNull(PostServiceApplication.class);
    }

}
