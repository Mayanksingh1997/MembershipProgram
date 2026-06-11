# FirstClub Membership — Demo Reference Data

Use this file for manual testing. Catalog APIs were removed; all static product data lives here and in `config/membership-catalog.yml`.

---

## Demo Users

| Email | Password | User ID | Orders | Monthly Value | Cohort |
|-------|----------|---------|--------|---------------|--------|
| user001@firstclub.co.in | password123 | user-001 | 5 | ₹1,200 | — |
| user002@firstclub.co.in | password123 | user-002 | 15 | ₹3,500 | — |
| user003@firstclub.co.in | password123 | user-003 | 20 | ₹6,000 | PREMIUM_COHORT |

---

## Membership Plans

| Code | Name | Price | Duration |
|------|------|-------|----------|
| MONTHLY | Monthly Plan | ₹99 | 30 days |
| QUARTERLY | Quarterly Plan | ₹249 | 90 days |
| YEARLY | Yearly Plan | ₹899 | 365 days |

---

## Membership Tiers & Eligibility

| Tier | Rank | Rules (all must pass) | Benefits |
|------|------|----------------------|----------|
| **SILVER** | 1 | Order count ≥ 0 | Free delivery, extra discount (default 5%, configurable) |
| **GOLD** | 2 | Order count ≥ 10 | Free delivery, extra discount (default 10%, configurable), exclusive deals |
| **PLATINUM** | 3 | Monthly order value ≥ ₹5,000 **and** user in `PREMIUM_COHORT` | Free delivery, extra discount (default 15%, configurable), exclusive deals, priority support |

### PREMIUM_COHORT

A marketing segment tag stored in `user_cohort` table. Only `user-003` has it in seed data. In production this would be assigned by CRM/marketing — not computed at runtime.

---

## API Endpoints

### Auth (public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/login` | Login → sets `accessToken` + `refreshToken` cookies |
| POST | `/api/v1/auth/logout` | Logout → clears cookies, deletes refresh token from DB |
| POST | `/api/v1/auth/refresh` | Get new access token only (refresh token stays until login or 7-day expiry) |

### Membership (protected — requires `accessToken` cookie)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/membership/subscribe` | Subscribe to plan + tier |
| GET | `/api/v1/membership/me` | Get active membership |
| PATCH | `/api/v1/membership/tier` | Upgrade / downgrade tier |
| POST | `/api/v1/membership/cancel` | Cancel membership |
| POST | `/api/v1/membership/evaluate-tier` | Re-evaluate tier from order stats |
| GET | `/api/v1/membership/benefits` | Resolved benefits for checkout |
| GET | `/api/v1/membership/order-stats` | Get current user's order stats |
| POST | `/api/v1/membership/order-stats` | Update mock order stats (demo only) |

---

## Example cURL Commands

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user001@firstclub.co.in","password":"password123"}' \
  -c cookies.txt

# Subscribe (Monthly + Silver)
curl -X POST http://localhost:8080/api/v1/membership/subscribe \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"planCode":"MONTHLY","tierCode":"SILVER","autoRenew":true,"paymentAmount":99,"paymentStrategy":"UPI"}'

# Get membership
curl http://localhost:8080/api/v1/membership/me -b cookies.txt

# Get checkout benefits
curl http://localhost:8080/api/v1/membership/benefits -b cookies.txt

# Get order stats (total orders + monthly order value)
curl http://localhost:8080/api/v1/membership/order-stats -b cookies.txt

# Upgrade tier (user-002 is eligible for Gold)
curl -X PATCH http://localhost:8080/api/v1/membership/tier \
  -H "Content-Type: application/json" \
  -b cookies.txt \
  -d '{"action":"UPGRADE","targetTierCode":"GOLD"}'

# Refresh access token (after 10 min expiry)
curl -X POST http://localhost:8080/api/v1/auth/refresh -b cookies.txt

# Logout
curl -X POST http://localhost:8080/api/v1/auth/logout -b cookies.txt
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `JWT_SIGNING_KEY` | (see application.yml) | Base64 HMAC signing key |
| `JWT_ISSUER` | firstclub | JWT `iss` claim |
| `JWT_AUDIENCE` | firstclub-api | JWT `aud` claim |
| `JWT_ACCESS_TOKEN_TTL_MINUTES` | 10 | Access token lifetime |
| `JWT_REFRESH_TOKEN_TTL_DAYS` | 7 | Refresh token lifetime |
| `MEMBERSHIP_TIER_SILVER_DISCOUNT_PERCENT` | 5 | Silver tier extra discount % |
| `MEMBERSHIP_TIER_GOLD_DISCOUNT_PERCENT` | 10 | Gold tier extra discount % |
| `MEMBERSHIP_TIER_PLATINUM_DISCOUNT_PERCENT` | 15 | Platinum tier extra discount % |
| `SPRING_PROFILES_ACTIVE` | mysql | Active database profile (currently only `mysql`) |
| `DB_NAME` | firstclub | Database name (used in JDBC URL) |
| `DB_URL` | (profile default) | Full JDBC URL override |
| `DB_USER` | firstclub | Database username |
| `DB_PASSWORD` | firstclub | Database password |

### Database profile (extensible)

Currently only MySQL is configured. Datasource settings live in `application-mysql.yml`, activated by `SPRING_PROFILES_ACTIVE=mysql`.

To add another database later (e.g. PostgreSQL):
1. Add driver dependency in `pom.xml`
2. Create `application-<dbname>.yml` with datasource URL, driver, Hibernate dialect, and Flyway location
3. Set `SPRING_PROFILES_ACTIVE=<dbname>` — no service/repository code changes needed (JPA interfaces stay the same)

```bash
# Default — MySQL
./mvnw spring-boot:run

# Explicit profile
SPRING_PROFILES_ACTIVE=mysql DB_NAME=firstclub ./mvnw spring-boot:run
```

### MySQL JDBC URL query parameters (local dev defaults)

Default URL in `application-mysql.yml`:

```
jdbc:mysql://localhost:3306/firstclub?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
```

| Parameter | Purpose |
|-----------|---------|
| `createDatabaseIfNotExist=true` | Creates the database on first connect — local dev convenience |
| `useSSL=false` | Disables SSL — local Docker MySQL has no SSL certificate |
| `allowPublicKeyRetrieval=true` | Required for MySQL 8 `caching_sha2_password` authentication over non-SSL connections |

> **Production:** Use SSL (`useSSL=true`), remove `createDatabaseIfNotExist`, and manage the database separately. Override via `DB_URL` env var.
