-- V12: Idempotency Keys
-- Scope: G2-T08 — chống double-click + retry mạng tạo bản ghi trùng cho
-- 2 endpoint: POST /chat/sessions/{sessionId}/messages và
-- POST /daily-checkins/{assignmentId}/answer.
--
-- Lý do bảng riêng (không phải UNIQUE constraint inline):
--   - Cần response snapshot (status + body) để trả về CÙNG response cũ
--     khi replay; constraint inline không có lưu trữ response.
--   - Áp dụng chung pattern cho mọi endpoint (G2-T09+ chỉ cần reuse
--     IdempotencyService, không ALTER bảng nghiệp vụ).
--   - Theo plan §3.6: chỉ record 2xx responses, 4xx/5xx không record.
--   - TTL 24h: sau khi hết hạn key tự coi như miss, request mới được tạo.
--
-- Column choices:
--   endpoint    : logical identifier dạng "POST:/chat/sessions/{sessionId}/messages"
--                 — path param được placeholder, KHÔNG dùng full URL vì
--                 URL có UUID runtime không stable cho key grouping.
--   key_value   : client UUID do crypto.randomUUID() sinh. Max 64 chars.
--   user_id     : lấy từ JWT principal (server trust), không từ client.
--   response_status : HTTP status tại thời điểm record (chỉ 2xx).
--   response_body   : JSONB snapshot của response body. Serialize consistent
--                 — service dùng Jackson ObjectMapper.
--   expires_at  : created_at + 24h. Service check expires_at > now() on read.
--   UNIQUE (user_id, endpoint, key_value) : natural key; race condition cuối
--                 cùng được bảo vệ bởi constraint này.

CREATE TABLE idempotency_keys (
    id              UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    endpoint        VARCHAR(64) NOT NULL,
    key_value       VARCHAR(64) NOT NULL,
    response_status SMALLINT    NOT NULL,
    response_body   JSONB       NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ NOT NULL,

    CONSTRAINT idempotency_keys_pkey PRIMARY KEY (id),
    CONSTRAINT idempotency_keys_user_fk
        FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT idempotency_keys_status_2xx_check
        CHECK (response_status BETWEEN 200 AND 299),
    CONSTRAINT idempotency_keys_endpoint_max_check
        CHECK (char_length(endpoint) <= 64),
    CONSTRAINT idempotency_keys_key_value_max_check
        CHECK (char_length(key_value) <= 64),
    CONSTRAINT idempotency_keys_natural_key_unique
        UNIQUE (user_id, endpoint, key_value)
);

-- Lookup: replay path (rare hot path but must be fast)
-- PK UNIQUE (user_id, endpoint, key_value) already gives btree index for this.

-- Secondary: cleanup / analytics — find all keys for a user+endpoint, newest first
CREATE INDEX idx_idempotency_user_endpoint_created
    ON idempotency_keys (user_id, endpoint, created_at DESC);

-- TTL cleanup job (future G3+): find all expired keys
CREATE INDEX idx_idempotency_expires_at
    ON idempotency_keys (expires_at);