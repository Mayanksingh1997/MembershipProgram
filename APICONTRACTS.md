# API Contracts

Base URL: `http://localhost:8080`

All error responses use:

```json
{
  "status": "ERROR",
  "message": "Human-readable message",
  "errorCode": "MACHINE_READABLE_CODE",
  "timestamp": "2026-06-11T10:00:00Z"
}
```

**Auth:** Protected endpoints require `accessToken` HttpOnly cookie set by `/api/v1/auth/login`.

---

## 1. POST `/api/v1/auth/login`

**Auth:** Public

### Request

```json
{
  "email": "user001@firstclub.co.in",
  "password": "password123"
}
```

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| email | string | yes | Valid email format |
| password | string | yes | Non-blank |

### Success — 200 OK

**Cookies set:** `accessToken` (10 min), `refreshToken` (7 days)

```json
{
  "status": "SUCCESS",
  "message": "Login successful",
  "timestamp": "2026-06-11T10:00:00Z",
  "userId": "user-001",
  "email": "user001@firstclub.co.in",
  "name": "Demo User One"
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 400 | VALIDATION_ERROR | Missing/invalid email or password |
| 401 | INVALID_CREDENTIALS | Wrong email or password |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 2. POST `/api/v1/auth/logout`

**Auth:** Protected — requires valid `accessToken` cookie

### Request

No body.

| Cookie | Required | Purpose |
|--------|----------|---------|
| `accessToken` | yes | Must be present and valid — enforced by JWT interceptor before logout runs |
| `refreshToken` | no | If sent, deleted from DB so the session cannot be refreshed again |

### Success — 200 OK

**Cookies cleared:** `accessToken`, `refreshToken` (both cleared in response even if only `accessToken` was sent)

```json
{
  "status": "SUCCESS",
  "message": "Logout successful",
  "timestamp": "2026-06-11T10:00:00Z"
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING | No `accessToken` cookie |
| 401 | TOKEN_EXPIRED | `accessToken` expired — call `/auth/refresh` first, then logout |
| 401 | INVALID_TOKEN | `accessToken` invalid or tampered |
| 503 | DATABASE_ERROR | Database unavailable during token deletion |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 3. POST `/api/v1/auth/refresh`

**Auth:** Public (requires `refreshToken` cookie)

### Request

No body. Cookie: `refreshToken`.

### Success — 200 OK

**Cookie updated:** new `accessToken` only (existing `refreshToken` unchanged until login or 7-day expiry)

```json
{
  "status": "SUCCESS",
  "message": "Token refreshed successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "userId": "user-001",
  "email": "user001@firstclub.co.in",
  "name": "Demo User One"
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | REFRESH_TOKEN_MISSING | `refreshToken` cookie not sent |
| 401 | INVALID_REFRESH_TOKEN | Token not found in DB |
| 401 | REFRESH_TOKEN_EXPIRED | Token past 7-day expiry — session ended, must login again |
| 401 | USER_NOT_FOUND | Email on token no longer exists |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 4. POST `/api/v1/membership/subscribe`

**Auth:** Protected (`accessToken` cookie)

### Request

**Headers:** `Idempotency-Key` (optional)

```json
{
  "planCode": "MONTHLY",
  "tierCode": "SILVER",
  "autoRenew": true,
  "paymentAmount": 99.00,
  "paymentStrategy": "UPI"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| planCode | enum | yes | `MONTHLY`, `QUARTERLY`, `YEARLY` |
| tierCode | enum | yes | `SILVER`, `GOLD`, `PLATINUM` |
| autoRenew | boolean | no | Default `true` |
| paymentAmount | decimal | yes | Must match selected plan price |
| paymentStrategy | enum | yes | `UPI`, `CARD` |

### Success — 201 Created

```json
{
  "status": "SUCCESS",
  "message": "Subscription created successfully",
  "timestamp": "2026-06-11T19:31:54.189097Z",
  "membershipId": 6,
  "userId": "user-001",
  "planCode": "MONTHLY",
  "tierCode": "SILVER",
  "membershipStatus": "ACTIVE",
  "startDate": "2026-06-12",
  "endDate": "2026-07-12",
  "daysRemaining": 30,
  "autoRenew": true,
  "paymentStatus": "Payment received successfully"
}
```

### Success — 200 OK (idempotent retry)

Same body with `"message": "Subscription already processed (idempotent)"` when same `Idempotency-Key` is resent. `paymentStatus` is omitted on idempotent retries.

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING | No `accessToken` cookie |
| 401 | TOKEN_EXPIRED | Access token expired — call `/auth/refresh` |
| 401 | INVALID_TOKEN | Tampered or invalid JWT |
| 401 | UNAUTHORIZED | User ID not found in request context |
| 400 | VALIDATION_ERROR | Missing planCode, tierCode, paymentAmount, or paymentStrategy |
| 404 | PLAN_NOT_FOUND | Invalid or inactive plan code |
| 404 | TIER_NOT_FOUND | Invalid or inactive tier code |
| 422 | INELIGIBLE_TIER | User does not meet tier eligibility rules |
| 422 | PAYMENT_AMOUNT_MISMATCH | paymentAmount does not match plan price |
| 409 | ACTIVE_MEMBERSHIP_EXISTS | User already has an active membership |
| 409 | CONCURRENT_MODIFICATION | Optimistic lock conflict — retry |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 5. GET `/api/v1/membership/me`

**Auth:** Protected

### Request

No body.

### Success — 200 OK

```json
{
  "status": "SUCCESS",
  "message": "Membership fetched successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "membershipId": 1,
  "userId": "user-001",
  "planCode": "MONTHLY",
  "tierCode": "SILVER",
  "membershipStatus": "ACTIVE",
  "startDate": "2026-06-11",
  "endDate": "2026-07-11",
  "daysRemaining": 30,
  "autoRenew": true
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 404 | MEMBERSHIP_NOT_FOUND | No active membership for user |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 6. PATCH `/api/v1/membership/tier`

**Auth:** Protected

### Request

```json
{
  "action": "UPGRADE",
  "targetTierCode": "GOLD"
}
```

| Field | Type | Required | Values |
|-------|------|----------|--------|
| action | enum | yes | `UPGRADE`, `DOWNGRADE` |
| targetTierCode | enum | yes | `SILVER`, `GOLD`, `PLATINUM` |

### Success — 200 OK

```json
{
  "status": "SUCCESS",
  "message": "Membership tier updated successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "membershipId": 1,
  "userId": "user-002",
  "planCode": "MONTHLY",
  "tierCode": "GOLD",
  "membershipStatus": "ACTIVE",
  "startDate": "2026-06-11",
  "endDate": "2026-07-11",
  "daysRemaining": 30,
  "autoRenew": true
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 400 | VALIDATION_ERROR | Missing action or targetTierCode |
| 404 | MEMBERSHIP_NOT_FOUND | No active membership |
| 404 | TIER_NOT_FOUND | Invalid tier code |
| 422 | INELIGIBLE_TIER | User does not meet target tier rules |
| 422 | INVALID_TIER_UPGRADE | Target tier rank not higher than current |
| 422 | INVALID_TIER_DOWNGRADE | Target tier rank not lower than current |
| 422 | INVALID_STATE_TRANSITION | Membership not in ACTIVE state |
| 409 | CONCURRENT_MODIFICATION | Optimistic lock conflict |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 7. POST `/api/v1/membership/cancel`

**Auth:** Protected

### Request

```json
{
  "immediate": false
}
```

| Field | Type | Required | Default |
|-------|------|----------|---------|
| immediate | boolean | no | `false` |

Empty body `{}` or no body is accepted (defaults apply).

### Success — 200 OK

```json
{
  "status": "SUCCESS",
  "message": "Membership cancelled successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "membershipId": 1,
  "userId": "user-001",
  "planCode": "MONTHLY",
  "tierCode": "SILVER",
  "membershipStatus": "CANCELLED",
  "startDate": "2026-06-11",
  "endDate": "2026-07-11",
  "daysRemaining": 0,
  "autoRenew": false
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 404 | MEMBERSHIP_NOT_FOUND | No active membership to cancel |
| 422 | INVALID_STATE_TRANSITION | Already cancelled or expired |
| 409 | CONCURRENT_MODIFICATION | Optimistic lock conflict |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 8. POST `/api/v1/membership/evaluate-tier`

**Auth:** Protected

### Request

No body. Re-evaluates tier from `user_order_aggregate` and `user_cohort` data.

### Success — 200 OK (tier changed)

```json
{
  "status": "SUCCESS",
  "message": "Tier evaluated and updated successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "membershipId": 1,
  "userId": "user-002",
  "planCode": "MONTHLY",
  "tierCode": "GOLD",
  "membershipStatus": "ACTIVE",
  "startDate": "2026-06-11",
  "endDate": "2026-07-11",
  "daysRemaining": 30,
  "autoRenew": true
}
```

### Success — 200 OK (tier unchanged)

Same shape with `"message": "Tier unchanged after evaluation"` and current tier unchanged.

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 404 | MEMBERSHIP_NOT_FOUND | No active membership |
| 422 | INVALID_STATE_TRANSITION | Membership not ACTIVE |
| 409 | CONCURRENT_MODIFICATION | Optimistic lock conflict |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 9. GET `/api/v1/membership/benefits`

**Auth:** Protected

### Request

No body.

### Success — 200 OK (active membership)

```json
{
  "status": "SUCCESS",
  "message": "Benefits resolved successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "userId": "user-002",
  "tierCode": "GOLD",
  "activeMembership": true,
  "freeDelivery": true,
  "extraDiscountPercent": 10,
  "exclusiveDealsAccess": true,
  "prioritySupport": false,
  "membershipExpiresAt": "2026-07-11"
}
```

### Success — 200 OK (no active membership)

```json
{
  "status": "SUCCESS",
  "message": "No active membership found",
  "timestamp": "2026-06-11T10:00:00Z",
  "userId": "user-001",
  "tierCode": null,
  "activeMembership": false,
  "freeDelivery": false,
  "extraDiscountPercent": 0,
  "exclusiveDealsAccess": false,
  "prioritySupport": false,
  "membershipExpiresAt": null
}
```

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 10. GET `/api/v1/membership/order-stats`

**Auth:** Protected

### Request

No body.

### Success — 200 OK

```json
{
  "status": "SUCCESS",
  "message": "Order stats fetched successfully",
  "timestamp": "2026-06-11T10:00:00Z",
  "userId": "user-002",
  "totalOrders": 15,
  "monthlyOrderValue": 3500.00,
  "lastOrderAt": "2026-06-10T14:30:00Z"
}
```

| Field | Type | Description |
|-------|------|-------------|
| userId | string | Authenticated user's external ID |
| totalOrders | integer | Lifetime order count |
| monthlyOrderValue | decimal | Order value for the current month |
| lastOrderAt | datetime \| null | Timestamp of most recent order update |

If the user has no order aggregate yet, `totalOrders` is `0`, `monthlyOrderValue` is `0.00`, and `lastOrderAt` is `null`.

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 404 | USER_NOT_FOUND | Authenticated user not in DB |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 11. POST `/api/v1/membership/order-stats`

**Auth:** Protected — demo endpoint to simulate order data for tier evaluation.

### Request

```json
{
  "totalOrders": 15,
  "monthlyOrderValue": 3500.00
}
```

| Field | Type | Required | Rules |
|-------|------|----------|-------|
| totalOrders | integer | yes | ≥ 0 |
| monthlyOrderValue | decimal | yes | ≥ 0 |

### Success — 204 No Content

Empty body.

### Failure scenarios

| HTTP | errorCode | When |
|------|-----------|------|
| 401 | ACCESS_TOKEN_MISSING / TOKEN_EXPIRED / INVALID_TOKEN / UNAUTHORIZED | Auth failures |
| 400 | VALIDATION_ERROR | Negative values or missing fields |
| 404 | USER_NOT_FOUND | Authenticated user not in DB |
| 503 | DATABASE_ERROR | Database unavailable |
| 500 | INTERNAL_ERROR | Unexpected server error |

---

## 12. GET `/actuator/health`

**Auth:** Public

### Success — 200 OK

```json
{
  "status": "UP"
}
```

### Failure scenarios

| HTTP | When |
|------|------|
| 503 | Application or database down |

---

## Global error codes reference

| errorCode | HTTP | Description |
|-----------|------|-------------|
| VALIDATION_ERROR | 400 | Bean validation failed |
| ACCESS_TOKEN_MISSING | 401 | `accessToken` cookie absent |
| TOKEN_EXPIRED | 401 | JWT `exp` passed |
| INVALID_TOKEN | 401 | JWT signature/claims invalid |
| INVALID_CREDENTIALS | 401 | Wrong login credentials |
| REFRESH_TOKEN_MISSING | 401 | `refreshToken` cookie absent |
| INVALID_REFRESH_TOKEN | 401 | Refresh token not in DB |
| REFRESH_TOKEN_EXPIRED | 401 | Refresh token expired |
| UNAUTHORIZED | 401 | Generic auth failure |
| USER_NOT_FOUND | 401/404 | User record missing |
| PLAN_NOT_FOUND | 404 | Plan code invalid |
| TIER_NOT_FOUND | 404 | Tier code invalid |
| MEMBERSHIP_NOT_FOUND | 404 | No active membership |
| ACTIVE_MEMBERSHIP_EXISTS | 409 | Duplicate subscribe attempt |
| CONCURRENT_MODIFICATION | 409 | Optimistic lock conflict |
| INELIGIBLE_TIER | 422 | Tier rules not met |
| PAYMENT_AMOUNT_MISMATCH | 422 | Payment amount does not match plan price |
| PAYMENT_STRATEGY_NOT_FOUND | 500 | Payment strategy not configured |
| INVALID_TIER_UPGRADE | 422 | Upgrade direction invalid |
| INVALID_TIER_DOWNGRADE | 422 | Downgrade direction invalid |
| INVALID_STATE_TRANSITION | 422 | Illegal lifecycle action |
| DATABASE_ERROR | 503 | DB operation failed |
| INTERNAL_ERROR | 500 | Unhandled exception |
| EVALUATOR_NOT_FOUND | 500 | Missing tier rule evaluator |
| STATE_NOT_FOUND | 500 | Missing state handler |
