package com.fanzone.reputation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Basic smoke test for reputation-service.
 * Full context load test requires Kafka + Redis + PostgreSQL infrastructure.
 */
class ReputationServiceApplicationTests {

    @Test
    void applicationClassExists() {
        assertNotNull(ReputationServiceApplication.class);
    }

}
