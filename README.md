# WebFlux + R2DBC(MySQL) Benchmark Server

This project implements the **WebFlux + R2DBC(MySQL)** variant for benchmark comparison.
It is designed to run against external MySQL (AWS RDS), with no DB container.

## Stack

- Java 21
- Spring Boot 3.x
- Spring WebFlux (`spring-boot-starter-webflux`)
- Spring Data R2DBC (`spring-boot-starter-data-r2dbc`)
- Spring Boot Actuator (`spring-boot-starter-actuator`)
- Micrometer Prometheus Registry (`micrometer-registry-prometheus`)
- R2DBC MySQL driver (`io.asyncer:r2dbc-mysql`)
- Gradle Wrapper + `bootJar`

## DB Schema (assumed)

```sql
CREATE TABLE bench_items (
  id BIGINT PRIMARY KEY,
  payload VARCHAR(100) NOT NULL,
  cnt BIGINT NOT NULL DEFAULT 0
);
```

## API Spec

### 1) GET `/api/v1/ping`

Response:

```json
{"ok":true}
```

### 2) GET `/api/v1/io/db/read?id=123&sleepMs=80`

Behavior:
1. `SELECT SLEEP(:sec)` where `sec = sleepMs / 1000.0`
2. `SELECT id, payload, cnt FROM bench_items WHERE id = :id`

Response:

```json
{"id":123,"payload":"...","cnt":0,"sleptMs":80}
```

### 3) POST `/api/v1/io/db/tx?sleepMs=30`

Body:

```json
{"id":123,"delta":1}
```

Behavior (single reactive transaction):
1. (optional) `SELECT SLEEP(:sec)`
2. `UPDATE bench_items SET cnt = cnt + :delta WHERE id = :id`
3. `SELECT cnt FROM bench_items WHERE id = :id`
4. commit

Response:

```json
{"id":123,"cnt":10,"delta":1,"sleptMs":30}
```

## Metrics (Prometheus)

- Endpoint: `GET /actuator/prometheus`
- Health: `GET /actuator/health`

Prometheus scrape example:

```yaml
scrape_configs:
  - job_name: webflux-r2dbc
    metrics_path: /actuator/prometheus
    static_configs:
      - targets: ['localhost:8080']
```

## Environment Variables

Required:
- `DB_HOST`
- `DB_PORT`
- `DB_NAME`
- `DB_USER`
- `DB_PASS`

Optional:
- `R2DBC_POOL_MAX` (default: `50`)
- `R2DBC_POOL_INIT` (default: `50`)
- `JAVA_OPTS` (default in Dockerfile):
  `-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError`

## Build

```bash
./gradlew clean bootJar
```

## Run (local)

```bash
DB_HOST=... DB_PORT=3306 DB_NAME=bench DB_USER=... DB_PASS=... ./gradlew bootRun
```

## Docker

Build image:

```bash
docker build -t webflux-r2dbc-bench:latest .
```

Unified run example:

```bash
docker run --rm -p 8080:8080 --cpus=1 --memory=1g --memory-swap=1g \
  -e DB_HOST=... -e DB_PORT=3306 -e DB_NAME=bench -e DB_USER=... -e DB_PASS=... \
  -e R2DBC_POOL_MAX=50 -e R2DBC_POOL_INIT=50 \
  -e JAVA_OPTS="-Xms512m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ExitOnOutOfMemoryError" \
  <image>
```
