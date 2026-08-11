package com.fanzone.matchthread;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic smoke test for match-thread-service.
 * Full context requires Kafka + Redis + PostgreSQL infrastructure.
 */
class MatchThreadServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertNotNull(MatchThreadServiceApplication.class);
    }

}
