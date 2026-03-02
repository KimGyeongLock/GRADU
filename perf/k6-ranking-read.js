import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 1,
            stages: [
                { duration: '1m', target: 20 },
                { duration: '1m', target: 30 },
                { duration: '1m', target: 50 },
                { duration: '1m', target: 0 },
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<800'],
    },
};


export default function () {
    const res = http.get(`${__ENV.BASE_URL}/api/v1/rankings/courses`, {
        headers: { Authorization: `Bearer ${__ENV.ACCESS_TOKEN}` },
    });

    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.2);
}