# K6 Load Tests

Performance and load testing scripts for Fanzone MVP.

## Prerequisites

- [K6](https://k6.io/docs/get-started/installation/) installed
- Services running (locally via Docker Compose or against staging)

## Running

```bash
# Feed endpoint — 500 concurrent users, p95 < 200ms target
k6 run load-tests/feed-load-test.js

# Match thread — 1000 concurrent comment submissions
k6 run load-tests/match-thread-load-test.js

# Auth — 100 concurrent logins
k6 run load-tests/auth-load-test.js
```

## Configuration

Set the `BASE_URL` environment variable to target different environments:

```bash
BASE_URL=https://api-staging.fanzone.dev k6 run load-tests/feed-load-test.js
```
