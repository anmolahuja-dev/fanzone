import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';

export const options = {
    stages: [
        { duration: '30s', target: 100 },  // Ramp up to 100 users
        { duration: '1m', target: 500 },   // Ramp up to 500 users
        { duration: '2m', target: 500 },   // Stay at 500 users
        { duration: '30s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<200'], // p95 response time < 200ms
        http_req_failed: ['rate<0.01'],   // Error rate < 1%
    },
};

// Pre-generated test JWT (replace with actual token generation in CI)
const TEST_TOKEN = __ENV.AUTH_TOKEN || 'test-token';

export default function () {
    const headers = {
        'Authorization': `Bearer ${TEST_TOKEN}`,
        'Content-Type': 'application/json',
    };

    // For You feed
    const forYouRes = http.get(`${BASE_URL}/api/v1/feeds/for-you?size=20`, { headers });
    check(forYouRes, {
        'for-you status is 200': (r) => r.status === 200,
        'for-you has items': (r) => JSON.parse(r.body).items !== undefined,
    });

    sleep(0.5);

    // Club feed
    const clubRes = http.get(`${BASE_URL}/api/v1/feeds/club?size=20`, { headers });
    check(clubRes, {
        'club status is 200': (r) => r.status === 200,
    });

    sleep(0.5);

    // Following feed
    const followingRes = http.get(`${BASE_URL}/api/v1/feeds/following?size=20`, { headers });
    check(followingRes, {
        'following status is 200': (r) => r.status === 200,
    });

    sleep(1);
}
