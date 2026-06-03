# Event Ledger System

A microservices-based system for processing financial events, built with Spring Boot.

---

## Architecture Overview

This project follows a microservices architecture with two services:

```
 Client
   │
   ▼
┌──────────────────────────────┐
│     Event Gateway Service    │  :8080
│                              │
│  - Accepts incoming events   │
│  - Validates & checks        │
│    idempotency               │
│  - Stores event in DB        │
│  - Exposes fetch APIs        │
└──────────────┬───────────────┘
               │  POST /accounts/{accountId}/transactions
               │  Header: X-Trace-Id
               ▼
┌──────────────────────────────┐
│     Account Service          │  :8081
│                              │
│  - Handles credit/debit      │
│    transactions              │
│  - Called by Gateway         │
│    via REST                  │
└──────────────────────────────┘
```

### Interaction Flow

```
Client → Event Gateway → Account Service → Gateway stores event → Response
```

1. Client sends a financial event to the **Event Gateway** (`POST /events`)
2. Gateway validates the request and checks for duplicate `eventId`
3. Gateway calls **Account Service** (`POST /accounts/{accountId}/transactions`) via `RestTemplate`
4. `X-Trace-Id` header is propagated for distributed tracing
5. Event is persisted and response is returned to the client
6. If Account Service is unavailable, the **Circuit Breaker** trips and a fallback `503` is returned immediately

---

## Setup Instructions

### Prerequisites

| Tool | Version |
|---|---|
| Java | 21+ |
| Maven | 3.8+ |
| Docker | 20+ *(optional, for containerised setup)* |
| Docker Compose | v2+ *(optional)* |

Verify your setup:
```bash
java -version
mvn -version
docker --version
```

### Install Dependencies

```bash
# Clone the repository
git clone <your-repo-url>
cd event-ledger

# Install dependencies for both services
cd event-gateway
mvn clean install -DskipTests

cd ../account-service
mvn clean install -DskipTests
```

---

## Starting Both Services

### Option 1 — Docker Compose (Recommended)

From the root of the project:

```bash
docker-compose up --build
```

Both services will start:
- Event Gateway → `http://localhost:8080`
- Account Service → `http://localhost:8081`

To stop:
```bash
docker-compose down
```

### Option 2 — Run Manually (Two Terminals)

**Terminal 1 — Start Account Service first:**
```bash
cd account-service
mvn spring-boot:run
# Running at http://localhost:8081
```

**Terminal 2 — Start Event Gateway:**
```bash
cd event-gateway
mvn spring-boot:run
# Running at http://localhost:8080
```

> ⚠️ Start the Account Service before the Gateway.

---

## Running the Tests

```bash
mvn test
```

### Test Coverage

| Test Class | Type | Scenarios Covered |
|---|---|---|
| `EventControllerTest` | Unit (`@WebMvcTest`) | Create event success, invalid amount (400), service unavailable (503), get by ID, get by accountId |
| `EventServiceTest` | Unit (Mockito) | Successful processing, duplicate event (idempotency), external service failure, validation |
| `EventIntegrationTest` | Integration (`MockRestServiceServer`) | Full Gateway → Account Service flow, `X-Trace-Id` header propagation |

---

## Resiliency Pattern — Circuit Breaker

**Pattern:** Circuit Breaker via [Resilience4j](https://resilience4j.readme.io/)

**Why Circuit Breaker?**

When the Account Service becomes unavailable, without protection the Gateway would keep sending requests — exhausting threads and causing a cascading failure across the system. The Circuit Breaker prevents this by monitoring failures and short-circuiting calls once a threshold is exceeded.

**States:**

```
CLOSED ──(failures exceed threshold)──► OPEN ──(wait duration)──► HALF-OPEN
  ▲                                                                     │
  └─────────────────(test calls succeed)───────────────────────────────┘
```

- **CLOSED** — Normal operation, all calls pass through
- **OPEN** — Calls are blocked; fallback method returns `503 Service Unavailable` immediately
- **HALF-OPEN** — A limited probe is sent to check if the service has recovered

**Implementation:**
```java
@CircuitBreaker(name = "accountService", fallbackMethod = "fallbackCreateEvent")
```

**Why not Retry alone?**
Retries help with brief transient glitches but worsen a sustained outage by amplifying traffic on a struggling service. The Circuit Breaker stops all retries once a failure pattern is detected, allowing the downstream service time to recover.

---

## API Reference

### Create Event
```
POST /events
```
```json
{
  "eventId": "evt-1",
  "accountId": "acct-1",
  "type": "CREDIT",
  "amount": 100.0,
  "eventTimestamp": "2026-05-15T10:00:00Z"
}
```

### Get Event by ID
```
GET /events/{id}
```

### Get Events by Account
```
GET /events?accountId={accountId}
```

---

## Additional Features

- **Idempotency** — Duplicate `eventId` returns existing event without reprocessing
- **Distributed Tracing** — `X-Trace-Id` generated via filter, stored in MDC, propagated to downstream
- **Metrics** — `events.processed.count` and `events.failed.count` via Micrometer
- **Global Exception Handling** — `IllegalArgumentException` → `400`, `ServiceUnavailableException` → `503`

---

## Assumptions

- Account Service runs on `localhost:8081`
- H2 in-memory database used for local development and testing
- No authentication implemented

---

## Future Improvements

- [ ] Replace `RestTemplate` with `WebClient`
- [ ] Add Retry mechanism with exponential backoff
- [ ] Add Kafka for async event streaming
- [ ] Add OpenTelemetry tracing
- [ ] Add JWT authentication
- [ ] Integration tests with Testcontainers

---

## Author

**Kamalkant Prajapati**
