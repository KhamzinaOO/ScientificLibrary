import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

export let options = {
  vus: 100,  // Количество виртуальных пользователей
  duration: '1m',  // Время выполнения теста
  thresholds: {
    'http_req_duration': ['p(95)<2000'],  // 95% запросов должны выполниться за 2 секунды
    'http_req_failed': ['rate<0.01'],  // Менее 1% запросов должны завершиться с ошибкой
  },
};

const BASE_URL = 'http://localhost:8080';

const searchMetrics = new Trend('search_duration');
const suggestMetrics = new Trend('suggest_duration');
const updateParamsMetrics = new Trend('update_params_duration');
const errorRate = new Rate('errors');

export default function () {
  // 1. Тестирование поиска
  const searchRes = http.get(`${BASE_URL}/search?page=1&pageSize=10&query=test`);
  check(searchRes, {
    'status 200': (r) => r.status === 200,
    'contains results': (r) => r.json().length > 0,
  }) || errorRate.add(1);
  searchMetrics.add(searchRes.timings.duration);

  // 2. Тестирование подсказок
  const suggestRes = http.get(`${BASE_URL}/suggest?type=author&query=john`);
  check(suggestRes, {
    'status 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  suggestMetrics.add(suggestRes.timings.duration);

  // 3. Обновление параметров ранжирования
  const updateRes = http.post(`${BASE_URL}/params/update`, JSON.stringify({
    bm25Parameter: 1.8,
    lambda: 0.1,
    alpha: 0.3,
    beta: 0.2,
    gamma: 0.4
  }), { headers: { 'Content-Type': 'application/json' } });

  check(updateRes, {
    'status 200': (r) => r.status === 200,
  }) || errorRate.add(1);
  updateParamsMetrics.add(updateRes.timings.duration);

  sleep(1);
}
