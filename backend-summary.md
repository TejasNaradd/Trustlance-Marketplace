# 🛡️ TrustLance Marketplace — Backend Comprehensive Architecture & API Summary

**Project:** TrustLance Decentralized/Escrow Freelance Marketplace  
**Architecture:** Spring Boot 4.x / Java 21 Microservices with Spring Cloud 2025.1.2  
**Security Model:** 3-Role RBAC (`BUYER`, `SELLER`, `ADMIN`) with Stateless JWT  
**AI Engine:** Groq Cloud LLM (`llama3-70b-8192`) via Spring AI 2.0.1  
**Message Broker:** Apache Kafka Event Streaming  
**Databases:** MySQL 8.0 (Relational with Flyway 12.4.0), MongoDB 8.0 (NoSQL), Redis 7.0 (Distributed Cache)  
**Object Storage:** MinIO S3 SDK  

---

## 📑 Table of Contents
1. [Architecture Overview & Microservices Topology](#1-architecture-overview--microservices-topology)
2. [Security, Authentication & The 3-Role System](#2-security-authentication--the-3-role-system)
3. [AI Service & Groq LLM Integration (Deep Dive)](#3-ai-service--groq-llm-integration-deep-dive)
4. [End-to-End User Journeys](#4-end-to-end-user-journeys)
5. [Complete REST API Directory & JSON Payloads](#5-complete-rest-api-directory--json-payloads)
6. [Kafka Event-Driven Architecture & Topics](#6-kafka-event-driven-architecture--topics)
7. [Database Schemas & Flyway Migrations](#7-database-schemas--flyway-migrations)
8. [Docker Deployment, Configuration & Swagger Access](#8-docker-deployment-configuration--swagger-access)

---

## 1. Architecture Overview & Microservices Topology

TrustLance is built as an enterprise-grade, distributed microservices ecosystem designed for high throughput, strict financial correctness, and decoupled event handling.

```
                                      ┌──────────────────┐
                                      │  Client / Frontend│
                                      └─────────┬────────┘
                                                │ (HTTP / JSON)
                                                ▼
                                    ┌───────────────────────┐
                                    │ API Gateway (Port 8080)│
                                    │ [Reactive WebFlux]    │
                                    │ - JWT Perimeter Auth  │
                                    │ - Rate Limiting       │
                                    └───────────┬───────────┘
                                                │
         ┌──────────────────────────────────────┼──────────────────────────────────────┐
         │                                      │                                      │
         ▼                                      ▼                                      ▼
┌──────────────────┐                  ┌──────────────────┐                  ┌──────────────────┐
│  Eureka Server   │                  │  Config Server   │                  │  Apache Kafka    │
│  (Port 8761)     │                  │  (Port 8888)     │                  │  (Port 9092)     │
│  Service Registry│                  │  Centralized YML │                  │  9 Event Topics  │
└──────────────────┘                  └──────────────────┘                  └──────────────────┘
         │
         ├──► user-service (Port 8081) ───────────► MySQL [user_db]
         ├──► marketplace-service (Port 8082) ────► MySQL [marketplace_db]
         ├──► contract-service (Port 8083) ───────► MySQL [contract_db] + Flyway
         ├──► payment-service (Port 8084) ────────► MySQL [payment_db] + Flyway
         ├──► escrow-service (Port 8085) ─────────► MySQL [escrow_db] + Flyway + Redis
         ├──► review-service (Port 8086) ─────────► MongoDB [review_db]
         ├──► dispute-service (Port 8087) ────────► MongoDB [dispute_db]
         ├──► notification-service (Port 8088) ───► MongoDB [notification_db] + SMTP
         └──► ai-service (Port 8089) ─────────────► MongoDB [ai_db] + MinIO + Groq LLM
```

### Microservices Inventory

| # | Service Name | Port | Technology Stack | Persistence / Storage | Primary Responsibility |
|---|---|---|---|---|---|
| 1 | **`config-server`** | `8888` | Spring Cloud Config Server | Native Classpath Config | Centralized configuration for all microservices |
| 2 | **`eureka-server`** | `8761` | Spring Cloud Netflix Eureka | In-Memory Registry | Service registration, heartbeat monitoring, discovery |
| 3 | **`api-gateway`** | `8080` | Spring Cloud Gateway (WebFlux) | Redis (Rate Limiter) | Single entry point, JWT token validation, header injection |
| 4 | **`user-service`** | `8081` | Spring Boot, Spring Security | MySQL (`user_db`) | Authentication (JWT, Google), user profile, role switching |
| 5 | **`marketplace-service`** | `8082` | Spring Boot, JPA | MySQL (`marketplace_db`) | Services catalog, gigs, categories, search, filtering |
| 6 | **`contract-service`** | `8083` | Spring Boot, JPA, Flyway | MySQL (`contract_db`) | Contracts, milestones, deliverables, revision cycles |
| 7 | **`payment-service`** | `8084` | Spring Boot, JPA, Flyway | MySQL (`payment_db`) | Payment intent lifecycle, provider abstraction, webhooks |
| 8 | **`escrow-service`** | `8085` | Spring Boot, JPA, Flyway | MySQL (`escrow_db`) + Redis | Fund holding, milestone releases, admin refunds, audit logs |
| 9 | **`review-service`** | `8086` | Spring Boot, Mongo JPA | MongoDB (`review_db`) | Dual-sided reviews and ratings (buyer ⇄ seller) |
| 10 | **`dispute-service`** | `8087` | Spring Boot, Mongo JPA | MongoDB (`dispute_db`) | Dispute raising, evidence uploads, admin resolution |
| 11 | **`notification-service`** | `8088` | Spring Boot, Mongo, JavaMail | MongoDB (`notification_db`) | Event listener, in-app notifications, email dispatch |
| 12 | **`ai-service`** | `8089` | Spring AI 2.0.1, Groq LLM | MongoDB (`ai_db`) + MinIO | AI document analysis, OCR extraction, fraud detection |

---

## 2. Security, Authentication & The 3-Role System

TrustLance enforces a strict **3-Role Role-Based Access Control (RBAC)** model.

```
                    ┌─────────────────────────┐
                    │      User Roles         │
                    └────────────┬────────────┘
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
   ┌───────────┐           ┌───────────┐           ┌───────────┐
   │   BUYER   │           │  SELLER   │           │   ADMIN   │
   └───────────┘           └───────────┘           └───────────┘
   - Hire Sellers          - List Services         - Arbitrate Disputes
   - Create Contracts      - Accept Contracts      - Manual Escrow Refunds
   - Fund Escrow           - Submit Deliverables   - Moderate Users & Gigs
   - Approve & Release     - Receive Payouts       - Platform Categories
```

### Role Definitions & Capabilities

1. **`BUYER`**:
   - Clients hiring talent on the platform.
   - Can create contracts, create payment intents, fund escrow milestones, review work, request revisions, release funds to sellers, raise disputes, and submit reviews.
2. **`SELLER`**:
   - Freelancers offering services.
   - Can create service listings/gigs, accept contract offers, submit milestone deliverables, and review buyers.
   - Requires verified status (via AI verification or Admin approval) to operate freely.
3. **`ADMIN`**:
   - Platform managers with full arbitration powers.
   - Can resolve disputes (order full/partial refunds or fund releases), manage category taxonomy, ban/activate users, and inspect platform metrics.

### Authentication & Token Propagation

1. **Token Generation**: `user-service` issues HMAC-SHA512 JWTs containing claims:
   - `sub`: User UUID (`id`)
   - `email`: User email address
   - `role`: One of `BUYER`, `SELLER`, `ADMIN`
   - `iat` & `exp`: Issued at and expiration timestamps (default: 24 hours)
2. **Gateway Verification (`JwtAuthFilter.java`)**:
   - Validates Bearer token at the perimeter (`api-gateway`).
   - If valid, injects downstream HTTP headers:
     - `X-User-Id: <UUID>`
     - `X-User-Role: BUYER|SELLER|ADMIN`
     - `X-User-Email: <email>`
3. **Microservice Security Context (`JwtAuthenticationFilter.java`)**:
   - Every microservice extracts the token or gateway headers into a Spring Security `UserPrincipal`.
   - Method-level security is enforced via `@PreAuthorize("hasRole('BUYER')")`, `@PreAuthorize("hasRole('SELLER')")`, or `@PreAuthorize("hasRole('ADMIN')")`.

---

## 3. AI Service & Groq LLM Integration (Deep Dive)

The `ai-service` provides asynchronous, high-accuracy document verification and OCR data extraction using the **Groq Llama 3 70B** model via the **Spring AI 2.0.1** framework.

```
 [User] ──(POST Document)──► [ai-service] ──(Save)──► [MinIO Storage]
                                   │
                             (Async Task)
                                   ▼
                         [Spring AI 2.0.1]
                         (ChatClient Prompt)
                                   │
                                   ▼
                         [Groq Cloud LLM]
                         (llama3-70b-8192)
                                   │
                            (JSON Response)
                                   ▼
 [user-service] ◄──(ai-events)── [Kafka] ◄──(Save)──► [MongoDB: ai_db]
```

### Technical Specifications
- **Model:** `llama3-70b-8192` (70-Billion Parameter Llama 3, 8192 Context Window).
- **Inference Provider:** Groq High-Speed LPU Inference (`https://api.groq.com/openai/v1`).
- **Spring AI Client:** `spring-ai-starter-model-openai` (Spring AI 2.0.1 compatible).
- **Document Storage:** MinIO S3 Object Storage (`trustlance-documents` bucket).
- **Execution Strategy:** Asynchronous `@Async` execution so client API calls return `202 ACCEPTED` immediately without blocking.

### Verification Prompt Architecture

The `GroqAiClient.java` constructs a system-guided prompt enforcing strict JSON output:

```
You are a document verification AI for a freelance marketplace platform (TrustLance).
Analyze the following {documentType} document and extract key information for identity verification.

Document reference: {objectStorageRef}
Document content/description: {documentContent}

Respond with ONLY a valid JSON object in this exact format:
{
  "status": "APPROVED" | "REJECTED" | "REQUIRES_MANUAL_REVIEW",
  "confidenceScore": <number between 0.0 and 1.0>,
  "extractedData": {
    "documentType": "<type>",
    "name": "<full name if found>",
    "idNumber": "<ID or document number if found>",
    "expiryDate": "<expiry date if found or null>",
    "issuingCountry": "<country if found or null>",
    "additionalFields": {}
  },
  "rejectionReason": "<reason if REJECTED, otherwise null>"
}
```

### Event Notification
When analysis completes:
1. Result is persisted in MongoDB `ai_verifications` collection.
2. Kafka event `VERIFICATION_COMPLETED` is broadcasted to the `ai-events` topic.
3. `user-service` consumes this event; if `APPROVED` with high confidence, the user's `isVerified` attribute is automatically updated to `true`.

---

## 4. End-to-End User Journeys

### A. The Buyer Journey
```
1. Register/Login (user-service) ➔ Obtain JWT (Role: BUYER)
2. Search Services (marketplace-service) ➔ Find Seller Gigs
3. Create Contract (contract-service) ➔ Define Milestones & Amount
4. Fund Milestone (payment-service) ➔ Create Payment Intent
5. Webhook Confirmed ➔ Escrow automatically locks funds (escrow-service)
6. Seller Submits Deliverable ➔ Buyer Reviews Work
7. Buyer Approves ➔ Escrow releases funds to Seller account
8. Leave Review (review-service) ➔ Rate Seller (1-5 Stars)
```

### B. The Seller Journey
```
1. Register (user-service) ➔ Upload ID/Passport (ai-service)
2. AI Verification Approved ➔ Upgrade to Role: SELLER
3. Create Gigs/Services (marketplace-service) ➔ Set Pricing Tiers
4. Receive Contract Offer (contract-service) ➔ Accept Contract
5. Work on Deliverable ➔ Submit Work with Attachments (contract-service)
6. Buyer Approves ➔ Escrow executes payout (escrow-service)
7. Leave Buyer Feedback (review-service)
```

### C. The Dispute & Arbitration Journey
```
1. Milestone Deliverable Disputed ➔ Buyer or Seller calls POST /api/v1/disputes
2. dispute-service triggers Kafka event ➔ Escrow automatically FREEZES funds
3. Both Parties submit Evidence & Attachments (POST /api/v1/disputes/{id}/evidence)
4. Admin reviews dispute details (GET /api/v1/admin/disputes)
5. Admin Resolves Dispute (POST /api/v1/admin/disputes/{id}/resolve):
   - Action: REFUND_BUYER ➔ Escrow refunds buyer
   - Action: RELEASE_TO_SELLER ➔ Escrow releases funds to seller
   - Action: SPLIT ➔ Custom split payout
6. Contract and Escrow state updated to RESOLVED / CLOSED
```

---

## 5. Complete REST API Directory & JSON Payloads

All endpoints are routed through the API Gateway at `http://localhost:8080`.

---

### 1. User & Authentication Service (`user-service` : `8081`)

#### `POST /api/v1/auth/register`
- **Access:** Public
- **Request:**
```json
{
  "email": "john.buyer@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "role": "BUYER"
}
```
- **Response (`201 Created`):**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "4f9d8e7b-1a2c-4d3e-8f9a-0b1c2d3e4f5a",
  "tokenType": "Bearer",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "john.buyer@example.com",
  "role": "BUYER"
}
```

#### `POST /api/v1/auth/login`
- **Access:** Public
- **Request:**
```json
{
  "email": "john.buyer@example.com",
  "password": "SecurePassword123!"
}
```
- **Response (`200 OK`):** Same as Register `AuthResponse`.

#### `POST /api/v1/auth/refresh`
- **Access:** Public
- **Request:**
```json
{
  "refreshToken": "4f9d8e7b-1a2c-4d3e-8f9a-0b1c2d3e4f5a"
}
```
- **Response (`200 OK`):** New `AuthResponse` with fresh JWT.

#### `GET /api/v1/users/me`
- **Access:** Authenticated (`BUYER`, `SELLER`, `ADMIN`)
- **Response (`200 OK`):**
```json
{
  "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "email": "john.buyer@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "BUYER",
  "status": "ACTIVE",
  "isVerified": true,
  "bio": "Tech entrepreneur looking for expert developers.",
  "avatarUrl": "https://minio.trustlance.com/avatars/user-1.jpg",
  "createdAt": "2026-09-01T10:00:00Z"
}
```

#### `PUT /api/v1/admin/users/{id}/status`
- **Access:** `ADMIN` only
- **Request:**
```json
{
  "status": "BANNED",
  "reason": "Violation of terms of service"
}
```
- **Response (`200 OK`):** Updated `UserResponse`.

---

### 2. Marketplace Service (`marketplace-service` : `8082`)

#### `POST /api/v1/services`
- **Access:** `SELLER` or `ADMIN`
- **Request:**
```json
{
  "title": "Full-Stack Spring Boot & React Application Development",
  "description": "I will build a high-performance, secure cloud microservice application.",
  "categoryId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "tags": ["spring-boot", "react", "microservices", "docker"],
  "pricingTiers": [
    {
      "tierName": "BASIC",
      "price": 250.00,
      "deliveryDays": 3,
      "description": "Single microservice API with documentation"
    },
    {
      "tierName": "PREMIUM",
      "price": 850.00,
      "deliveryDays": 10,
      "description": "Full microservice suite with React frontend and Docker setup"
    }
  ]
}
```
- **Response (`201 Created`):**
```json
{
  "id": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "sellerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "title": "Full-Stack Spring Boot & React Application Development",
  "status": "ACTIVE",
  "rating": 5.0,
  "reviewCount": 12,
  "createdAt": "2026-09-05T12:00:00Z"
}
```

#### `GET /api/v1/services`
- **Access:** Public (Browsing)
- **Query Params:** `?search=spring&categoryId=...&page=0&size=20`
- **Response (`200 OK`):** Paginated `Page<ServiceResponse>`.

---

### 3. Contract & Milestone Service (`contract-service` : `8083`)

#### `POST /api/v1/contracts`
- **Access:** `BUYER` only
- **Request:**
```json
{
  "sellerId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "serviceId": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "title": "Build Payment Integration Service",
  "description": "Implement Stripe/Razorpay webhook handling with idempotency.",
  "totalAmount": 500.00,
  "currency": "USD",
  "milestones": [
    {
      "title": "Milestone 1: Database & Architecture Setup",
      "amount": 200.00,
      "dueDate": "2026-09-15T00:00:00Z"
    },
    {
      "title": "Milestone 2: Webhooks & End-to-End Tests",
      "amount": 300.00,
      "dueDate": "2026-09-25T00:00:00Z"
    }
  ]
}
```
- **Response (`201 Created`):**
```json
{
  "id": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "buyerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sellerId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "status": "PENDING_ACCEPTANCE",
  "totalAmount": 500.00,
  "currency": "USD",
  "createdAt": "2026-09-05T14:30:00Z"
}
```

#### `POST /api/v1/contracts/{id}/accept`
- **Access:** `SELLER` only (must match `sellerId`)
- **Response (`200 OK`):** Contract status transitions to `ACTIVE`.

#### `POST /api/v1/milestones/{id}/submit`
- **Access:** `SELLER` only
- **Request:**
```json
{
  "deliverableUrl": "https://github.com/trustlance/payment-module",
  "notes": "Completed initial MySQL schema and Stripe webhook listeners."
}
```
- **Response (`200 OK`):** Milestone status transitions to `SUBMITTED`.

#### `POST /api/v1/milestones/{id}/approve`
- **Access:** `BUYER` only
- **Response (`200 OK`):** Milestone status transitions to `APPROVED`; triggers Escrow release event.

---

### 4. Payment Service (`payment-service` : `8084`)

#### `POST /api/v1/payments/intents`
- **Access:** `BUYER` only
- **Request:**
```json
{
  "contractId": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "milestoneId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "amount": 200.00,
  "currency": "USD",
  "provider": "MOCK_PROVIDER",
  "idempotencyKey": "pay_idemp_987654321"
}
```
- **Response (`201 Created`):**
```json
{
  "id": "5e6f7a8b-9c0d-1e2f-3a4b-5c6d7e8f9a0b",
  "milestoneId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "amount": 200.00,
  "currency": "USD",
  "status": "REQUIRES_PAYMENT",
  "clientSecret": "pi_mock_secret_12345",
  "createdAt": "2026-09-05T15:00:00Z"
}
```

#### `POST /api/v1/payments/webhooks/{provider}`
- **Access:** Public (Protected by Header Signature `X-Payment-Signature`)
- **Response (`200 OK`):** Webhook payload processed; broadcasts `PAYMENT_SUCCESS` to Kafka.

---

### 5. Escrow Service (`escrow-service` : `8085`)

#### `GET /api/v1/escrow/{id}`
- **Access:** Authenticated (Contract Buyer, Seller, or `ADMIN`)
- **Response (`200 OK`):**
```json
{
  "id": "6f7a8b9c-0d1e-2f3a-4b5c-6d7e8f9a0b1c",
  "milestoneId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "contractId": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "buyerId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "sellerId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "amount": 200.00,
  "currency": "USD",
  "status": "HELD",
  "version": 1,
  "createdAt": "2026-09-05T15:05:00Z"
}
```

#### `POST /api/v1/escrow/{id}/release`
- **Access:** `BUYER` or `ADMIN`
- **Response (`200 OK`):** Status transitions from `HELD` to `RELEASED`; payout logged to `escrow_transactions`.

#### `POST /api/v1/escrow/{id}/freeze`
- **Access:** Authenticated (Invoked automatically when a dispute is opened)
- **Response (`200 OK`):** Status transitions to `FROZEN`.

#### `POST /api/v1/escrow/{id}/refund`
- **Access:** `ADMIN` only
- **Response (`200 OK`):** Status transitions to `REFUNDED`; funds returned to Buyer.

---

### 6. Review & Rating Service (`review-service` : `8086`)

#### `POST /api/v1/reviews`
- **Access:** Authenticated (`BUYER` or `SELLER`)
- **Request:**
```json
{
  "contractId": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "targetUserId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "rating": 5,
  "comment": "Outstanding developer. Clean code, fast delivery, and great communication!"
}
```
- **Response (`201 Created`):**
```json
{
  "id": "66da00000000000000000001",
  "authorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "targetUserId": "8a7b6c5d-4e3f-2a1b-0c9d-8e7f6a5b4c3d",
  "rating": 5,
  "comment": "Outstanding developer. Clean code, fast delivery, and great communication!",
  "createdAt": "2026-09-05T16:00:00Z"
}
```

---

### 7. Dispute Service (`dispute-service` : `8087`)

#### `POST /api/v1/disputes`
- **Access:** Authenticated (`BUYER` or `SELLER`)
- **Request:**
```json
{
  "contractId": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "milestoneId": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "reason": "Deliverable does not match agreed specifications and contains critical bugs.",
  "evidenceDescription": "Attached postman test results and error logs."
}
```
- **Response (`201 Created`):**
```json
{
  "id": "66da11111111111111111111",
  "contractId": "1c2d3e4f-5a6b-7c8d-9e0f-1a2b3c4d5e6f",
  "initiatorId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "status": "OPEN",
  "createdAt": "2026-09-05T16:30:00Z"
}
```

#### `POST /api/v1/admin/disputes/{id}/resolve`
- **Access:** `ADMIN` only
- **Request:**
```json
{
  "resolution": "REFUND_BUYER",
  "adminNotes": "Evidence shows required milestone deliverables were not provided.",
  "buyerRefundPercentage": 100.0,
  "sellerPayoutPercentage": 0.0
}
```
- **Response (`200 OK`):** Status transitions to `RESOLVED`; sends event to Kafka to release/refund Escrow.

---

### 8. Notification Service (`notification-service` : `8088`)

#### `GET /api/v1/notifications`
- **Access:** Authenticated
- **Query Params:** `?unreadOnly=true&page=0&size=20`
- **Response (`200 OK`):**
```json
{
  "content": [
    {
      "id": "66da22222222222222222222",
      "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "title": "Payment Confirmed",
      "message": "Funds for Milestone 1 are now securely held in Escrow.",
      "type": "ESCROW_HELD",
      "read": false,
      "createdAt": "2026-09-05T15:06:00Z"
    }
  ],
  "totalElements": 1,
  "totalPages": 1
}
```

---

### 9. AI Service (`ai-service` : `8089`)

#### `POST /api/v1/ai/verifications`
- **Access:** Authenticated (`BUYER` or `SELLER`)
- **Request:**
```json
{
  "documentType": "PASSPORT",
  "objectStorageRef": "minio://trustlance-documents/passport-user-123.jpg"
}
```
- **Response (`202 Accepted`):**
```json
{
  "id": "66da33333333333333333333",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "documentType": "PASSPORT",
  "objectStorageRef": "minio://trustlance-documents/passport-user-123.jpg",
  "status": "PENDING",
  "createdAt": "2026-09-05T17:00:00Z"
}
```

#### `GET /api/v1/ai/verifications/{id}`
- **Access:** Authenticated (Owner or `ADMIN`)
- **Response (`200 OK` - Post AI Analysis):**
```json
{
  "id": "66da33333333333333333333",
  "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "documentType": "PASSPORT",
  "status": "APPROVED",
  "confidenceScore": 0.96,
  "extractedData": "{\"name\":\"John Doe\",\"idNumber\":\"P123456789\",\"expiryDate\":\"2030-05-12\",\"issuingCountry\":\"USA\"}",
  "rejectionReason": null,
  "createdAt": "2026-09-05T17:00:00Z"
}
```

---

## 6. Kafka Event-Driven Architecture & Topics

| Topic Name | Publisher | Consumer(s) | Event Payload / Action |
|---|---|---|---|
| `user-events` | `user-service` | `notification-service` | `USER_REGISTERED`, `PROFILE_UPDATED`, `ROLE_CHANGED` |
| `marketplace-events` | `marketplace-service` | `notification-service` | `SERVICE_CREATED`, `SERVICE_UPDATED`, `SERVICE_DELETED` |
| `contract-events` | `contract-service` | `escrow-service`, `notification-service` | `CONTRACT_CREATED`, `CONTRACT_ACCEPTED`, `CONTRACT_CANCELLED` |
| `payment-events` | `payment-service` | `escrow-service`, `notification-service` | `PAYMENT_SUCCESS` ➔ Triggers Escrow fund lock |
| `escrow-events` | `escrow-service` | `contract-service`, `notification-service` | `FUNDS_HELD`, `FUNDS_RELEASED`, `FUNDS_FROZEN`, `FUNDS_REFUNDED` |
| `dispute-events` | `dispute-service` | `escrow-service`, `notification-service` | `DISPUTE_RAISED` (Freezes escrow), `DISPUTE_RESOLVED` |
| `review-events` | `review-service` | `user-service`, `notification-service` | `REVIEW_SUBMITTED` ➔ Recalculates seller average rating |
| `ai-events` | `ai-service` | `user-service`, `notification-service` | `VERIFICATION_COMPLETED` ➔ Sets user `isVerified = true` |
| `notification-events` | All Services | `notification-service` | Dispatches in-app alerts and trigger SMTP emails |

---

## 7. Database Schemas & Flyway Migrations

### Relational Schema (MySQL 8.0)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 user_db                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ users: (id CHAR(36) PK, email, password_hash, role, status, is_verified)    │
│ refresh_tokens: (id CHAR(36) PK, user_id FK, token, expiry_date)           │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                             marketplace_db                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│ categories: (id CHAR(36) PK, name, slug, description)                       │
│ services: (id CHAR(36) PK, seller_id, category_id FK, title, status)       │
│ pricing_tiers: (id CHAR(36) PK, service_id FK, tier_name, price, days)      │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              contract_db                                    │
├─────────────────────────────────────────────────────────────────────────────┤
│ contracts: (id CHAR(36) PK, buyer_id, seller_id, title, status, amount)    │
│ milestones: (id CHAR(36) PK, contract_id FK, title, amount, status)        │
│ submissions: (id CHAR(36) PK, milestone_id FK, deliverable_url, notes)     │
│ state_history: (id CHAR(36) PK, contract_id FK, from_state, to_state)       │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                              payment_db                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ payment_intents: (id CHAR(36) PK, contract_id, milestone_id, amount, status)│
│ webhook_events: (id CHAR(36) PK, provider, provider_event_id, payload)     │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                               escrow_db                                     │
├─────────────────────────────────────────────────────────────────────────────┤
│ escrows: (id CHAR(36) PK, milestone_id UNIQUE, buyer_id, seller_id, status) │
│ escrow_transactions: (id CHAR(36) PK, escrow_id, amount, actor_role, reason)│
└─────────────────────────────────────────────────────────────────────────────┘
```

### Guaranteed Flyway Execution Pattern
In `contract-service`, `payment-service`, and `escrow-service`, Hibernate is set to strict `ddl-auto: validate`. An explicit configuration bean guarantees Flyway migration completes before Hibernate schema validation:

```java
@Configuration
public class DatabaseMigrationConfig {
    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();
    }
}
```

---

## 8. Docker Deployment, Configuration & Swagger Access

### Environment Variables (`.env`)
```bash
# Groq AI
GROQ_API_KEY=gsk_your_actual_groq_api_key
GROQ_MODEL=llama3-70b-8192
GROQ_BASE_URL=https://api.groq.com/openai/v1

# Security
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970

# Databases
MYSQL_HOST=mysql
MYSQL_PORT=3306
MYSQL_ROOT_PASSWORD=rcoem
SPRING_DATA_MONGODB_URI=mongodb://mongodb:27017/
REDIS_HOST=redis
REDIS_PORT=6379

# Kafka & Storage
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
MINIO_ENDPOINT=http://minio:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
```

### Docker Quickstart
```bash
# 1. Start all infrastructure and microservices
docker compose up -d --build

# 2. Check container status
docker ps
```

### Swagger UI & Interactive API Documentation

| Service | Swagger UI URL | OpenAPI JSON Spec |
|---|---|---|
| **User Service** | `http://localhost:8081/swagger-ui.html` | `/v3/api-docs` |
| **Marketplace Service** | `http://localhost:8082/swagger-ui.html` | `/v3/api-docs` |
| **Contract Service** | `http://localhost:8083/swagger-ui.html` | `/v3/api-docs` |
| **Payment Service** | `http://localhost:8084/swagger-ui.html` | `/v3/api-docs` |
| **Escrow Service** | `http://localhost:8085/swagger-ui.html` | `/v3/api-docs` |
| **Review Service** | `http://localhost:8086/swagger-ui.html` | `/v3/api-docs` |
| **Dispute Service** | `http://localhost:8087/swagger-ui.html` | `/v3/api-docs` |
| **Notification Service** | `http://localhost:8088/swagger-ui.html` | `/v3/api-docs` |
| **AI Service** | `http://localhost:8089/swagger-ui.html` | `/v3/api-docs` |

---

*TrustLance Backend Architecture — Designed for Reliability, Financial Integrity, and Scale.*
