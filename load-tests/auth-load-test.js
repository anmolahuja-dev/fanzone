import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';

export const options = {
    stages: [
        { duration: '15s', target: 50 },   // Ramp up
        { duration: '1m', target: 100 },   // 100 concurrent logins
        { duration: '1m', target: 100 },   // Sustain
        { duration: '15s', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // p95 < 500ms for auth
        http_req_failed: ['rate<0.05'],    // Allow some failures (lockout, etc.)
    },
};

export default function () {
    const headers = { 'Content-Type': 'application/json' };

    // Login attempt
    const loginPayload = JSON.stringify({
        email: `user${__VU}@fanzone.test`,
        password: 'TestPass123',
    });

    const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, { headers });

    check(loginRes, {
        'login responds': (r) => r.status === 200 || r.status === 401,
    });

    sleep(1);

    // Token refresh (if login succeeded)
    if (loginRes.status === 200) {
        const body = JSON.parse(loginRes.body);
        const refreshPayload = JSON.stringify({
            refreshToken: body.refreshToken,
        });

        const refreshRes = http.post(`${BASE_URL}/api/v1/auth/refresh`, refreshPayload, { headers });
        check(refreshRes, {
            'refresh responds': (r) => r.status === 200 || r.status === 401,
        });
    }

    sleep(0.5);
}
