# Integration Tests

End-to-end integration tests using Testcontainers (PostgreSQL, Redis, Kafka).

## Prerequisites

- Docker running locally
- Java 21
- Maven

## Running

```bash
# From project root:
./scripts/build.sh all  # First build all services

# Then run integration tests:
cd integration-tests
mvn verify -s ../.m2/settings.xml
```

## Test Flows

1. **Auth flow**: register → verify email → login → JWT → access protected endpoint
2. **Post flow**: create post → appears in feed → comment → upvote → reputation update
3. **Match thread flow**: join → live comments (WebSocket) → player ratings → MOTM
4. **Follow flow**: follow user → posts appear in Following feed
5. **Moderation flow**: toxic content → flagged
