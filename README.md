# TrustLance — AI-Assisted Freelance Marketplace Backend

A production-style microservices backend built with **Spring Boot 4.1.0**, **Spring Cloud 2025.1.2**, **Java 21**, **MySQL**, **MongoDB**, **Redis**, **Kafka**, and **MinIO**.

## Architecture

```
┌─────────────┐     ┌───────────────┐     ┌─────────────┐
│  API Gateway │────▶│  Eureka Server│◀────│Config Server│
│   (8080)     │     │   (8761)      │     │   (8888)    │
└──────┬───────┘     └───────────────┘     └─────────────┘
       │
       ├── user-service          (8081)  MySQL   – Auth, JWT, Profiles
       ├── marketplace-service   (8082)  MySQL   – Services, Categories
       ├── contract-service      (8083)  MySQL   – Contracts, Milestones
       ├── payment-service       (8084)  MySQL   – Provider Intents, Webhooks
       ├── escrow-service        (8085)  MySQL   – Fund/Freeze/Release/Refund
       ├── review-service        (8086)  MongoDB – Reviews, Ratings
       ├── dispute-service       (8087)  MongoDB – Disputes, Evidence
       ├── notification-service  (8088)  MongoDB – Kafka-driven Notifications
       └── ai-service            (8089)  MongoDB – AI Verification
```

## Prerequisites

- **Java 21** (e.g., Eclipse Temurin)
- **Maven 3.9+**
- **Docker** & **Docker Compose**

## Quick Start

### 1. Clone & Configure

```bash
git clone <repository-url>
cd trustLance-MarketPlace
cp .env.example .env
# Edit .env with your real credentials for production
```

### 2. Start Everything

```bash
docker compose up --build
```

This starts all services and infrastructure:
- **MySQL** (3306) — 5 databases: user_db, marketplace_db, contract_db, payment_db, escrow_db
- **MongoDB** (27017) — 4 databases: review_db, dispute_db, notification_db, ai_db
- **Redis** (6379)
- **Kafka** (9092) — KRaft mode (no Zookeeper)
- **MinIO** (9000/9001)

### 3. Verify

| Check | URL |
|-------|-----|
| Eureka Dashboard | http://localhost:8761 |
| Config Server Health | http://localhost:8888/actuator/health |
| Gateway Health | http://localhost:8080/actuator/health |
| MinIO Console | http://localhost:9001 |

## Startup Order

1. `config-server` (must be healthy first)
2. `eureka-server` (depends on config-server)
3. `api-gateway` + all business services (depend on eureka + their databases)

Docker Compose handles this automatically via `depends_on` with health checks.

## API Flow (E2E)

```
1. POST /api/v1/auth/register     → Create buyer/seller account
2. POST /api/v1/auth/login        → Get JWT tokens
3. GET  /api/v1/services          → Browse marketplace
4. POST /api/v1/contracts         → Buyer creates contract
5. POST /api/v1/contracts/{id}/accept → Seller accepts
6. POST /api/v1/contracts/{id}/milestones → Add milestones
7. POST /api/v1/payments/intents  → Fund milestone
8. [Webhook] /api/v1/payments/webhooks/{provider} → Provider confirms
9. → Kafka: PAYMENT_SUCCESS → Escrow FUNDED
10. POST /api/v1/milestones/{id}/submit → Seller submits work
11. → Kafka: MILESTONE_SUBMITTED → AI verification
12. POST /api/v1/milestones/{id}/approve → Buyer approves
13. → Kafka: MILESTONE_APPROVED
14. POST /api/v1/escrow/{id}/release → Release funds
15. → Kafka: ESCROW_RELEASED
16. POST /api/v1/reviews → Leave review
17. → Contract COMPLETED when all milestones released
```

## Gateway Routes

| Route | Service |
|-------|---------|
| `/api/v1/auth/**` | user-service |
| `/api/v1/users/**` | user-service |
| `/api/v1/admin/users/**` | user-service |
| `/api/v1/services/**` | marketplace-service |
| `/api/v1/contracts/**` | contract-service |
| `/api/v1/milestones/**` | contract-service |
| `/api/v1/payments/**` | payment-service |
| `/api/v1/escrow/**` | escrow-service |
| `/api/v1/reviews/**` | review-service |
| `/api/v1/disputes/**` | dispute-service |
| `/api/v1/notifications/**` | notification-service |
| `/api/v1/ai/**` | ai-service |

## Kafka Topics

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `user-events` | user-service | notification |
| `marketplace-events` | marketplace-service | ai, notification |
| `contract-events` | contract-service | notification, ai, payment |
| `payment-events` | payment-service | escrow, notification |
| `escrow-events` | escrow-service | notification |
| `dispute-events` | dispute-service | escrow, notification |
| `review-events` | review-service | notification |

## Security

- **JWT**: JJWT 0.13.0 with HMAC-SHA256 signing
- **Gateway**: Validates JWT, extracts `sub`/`role`, forwards via `X-User-Id`/`X-User-Role` headers
- **Services**: Each validates JWT at its own trust boundary (fallback to direct JWT validation)
- **RBAC**: `@PreAuthorize` annotations on all endpoints
- **Roles**: `BUYER`, `SELLER`, `ADMIN`

## Troubleshooting

| Issue | Solution |
|-------|---------|
| Config server not starting | Ensure port 8888 is free |
| Services fail to register | Check eureka-server is healthy first |
| Database connection errors | Verify MySQL/MongoDB containers are running |
| Kafka connection refused | Kafka needs ~30s to start in KRaft mode |
| Flyway migration errors | Check SQL syntax; drop and recreate DB if needed |

## Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 LTS | Language |
| Spring Boot | 4.1.0 | Application framework |
| Spring Cloud | 2025.1.2 | Microservices infrastructure |
| MySQL | 8.0 | Relational data (user, marketplace, contract, payment, escrow) |
| MongoDB | 7.0 | Document data (review, dispute, notification, ai) |
| Redis | 7 | Cache, rate limiting, distributed locks |
| Kafka | 3.9 | Event streaming |
| MinIO | Latest | S3-compatible object storage |
| JJWT | 0.13.0 | JWT processing |
| Flyway | Latest | Database migrations |
| Docker | Latest | Containerization |
