CREATE TABLE user_account (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_user_id    VARCHAR(64) NOT NULL UNIQUE,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    name                VARCHAR(128) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_token (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL,
    token           VARCHAR(512) NOT NULL UNIQUE,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_refresh_token_email (email)
);

CREATE TABLE user_cohort (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    cohort_code     VARCHAR(64) NOT NULL,
    assigned_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_cohort_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT uq_user_cohort UNIQUE (user_id, cohort_code)
);

CREATE TABLE user_order_aggregate (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                 BIGINT NOT NULL UNIQUE,
    total_orders            INT NOT NULL DEFAULT 0,
    monthly_order_value     DECIMAL(12, 2) NOT NULL DEFAULT 0,
    last_order_at           TIMESTAMP NULL,
    updated_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_agg_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE user_membership (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    plan_code       VARCHAR(32) NOT NULL,
    tier_code       VARCHAR(32) NOT NULL,
    status          VARCHAR(32) NOT NULL,
    start_date      DATE NOT NULL,
    end_date        DATE NOT NULL,
    auto_renew      BOOLEAN NOT NULL DEFAULT TRUE,
    version         BIGINT NOT NULL DEFAULT 0,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES user_account(id)
);

CREATE TABLE membership_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    membership_id   BIGINT NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    old_value_json  JSON,
    new_value_json  JSON,
    triggered_by    VARCHAR(64),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_membership FOREIGN KEY (membership_id) REFERENCES user_membership(id)
);

CREATE TABLE subscription_idempotency (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    membership_id   BIGINT NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_idempotency_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_idempotency_membership FOREIGN KEY (membership_id) REFERENCES user_membership(id)
);
