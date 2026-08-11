import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8084';
const MATCH_ID = __ENV.MATCH_ID || '00000000-0000-0000-0000-000000000001';

export const options = {
    stages: [
        { duration: '30s', target: 200 },   // Ramp up
        { duration: '1m', target: 1000 },   // Ramp to 1000 concurrent users
        { duration: '2m', target: 1000 },   // Sustain
        { duration: '30s', target: 0 },     // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],   // p95 < 500ms for match threads
        http_req_failed: ['rate<0.05'],     // Allow up to 5% failures (rate limiting)
    },
};

const TEST_TOKEN = __ENV.AUTH_TOKEN || 'test-token';

export default function () {
    const headers = {
        'Authorization': `Bearer ${TEST_TOKEN}`,
        'Content-Type': 'application/json',
    };

    // Post a comment to match thread
    const commentPayload = JSON.stringify({
        content: `Live comment from user ${__VU} at ${Date.now()}`,
    });

    const commentRes = http.post(
        `${BASE_URL}/api/v1/match-threads/${MATCH_ID}/comments`,
        commentPayload,
        { headers }
    );

    check(commentRes, {
        'comment posted or rate limited': (r) => r.status === 201 || r.status === 422,
    });

    sleep(0.1);

    // Get comments (read load)
    const getRes = http.get(
        `${BASE_URL}/api/v1/match-threads/${MATCH_ID}/comments`,
        { headers }
    );

    check(getRes, {
        'get comments status 200': (r) => r.status === 200,
    });

    sleep(0.5);
}
