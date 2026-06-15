CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE organizations (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name text NOT NULL,
    slug text UNIQUE NOT NULL,
    billing_email text,
    default_language text NOT NULL DEFAULT 'en',
    status text NOT NULL DEFAULT 'ACTIVE',
    free_grant_applied boolean NOT NULL DEFAULT false,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_organizations_default_language CHECK (default_language IN ('en', 'fr')),
    CONSTRAINT ck_organizations_status CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE INDEX ix_organizations_status ON organizations(status);

CREATE TABLE users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email text UNIQUE NOT NULL,
    display_name text,
    avatar_url text,
    oidc_provider text NOT NULL,
    oidc_subject text NOT NULL,
    is_platform_admin boolean NOT NULL DEFAULT false,
    last_login_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_users_oidc_provider CHECK (oidc_provider IN ('GOOGLE', 'MICROSOFT', 'GITHUB', 'APPLE')),
    CONSTRAINT ux_users_oidc_provider_subject UNIQUE (oidc_provider, oidc_subject)
);

CREATE INDEX ix_users_email ON users(email);

CREATE TABLE organization_members (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role text NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_organization_members_role CHECK (role IN ('OWNER', 'ADMIN', 'DEVELOPER', 'BILLING')),
    CONSTRAINT ux_organization_members_org_user UNIQUE (organization_id, user_id)
);

CREATE UNIQUE INDEX ux_organization_members_one_owner
    ON organization_members(organization_id)
    WHERE role = 'OWNER';

CREATE TABLE api_keys (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    name text NOT NULL,
    key_prefix text NOT NULL,
    key_hash char(64) UNIQUE NOT NULL,
    status text NOT NULL DEFAULT 'ACTIVE',
    last_used_at timestamptz,
    rotated_from uuid REFERENCES api_keys(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    revoked_at timestamptz,
    CONSTRAINT ck_api_keys_status CHECK (status IN ('ACTIVE', 'REVOKED', 'ROTATED'))
);

CREATE INDEX ix_api_keys_organization_status ON api_keys(organization_id, status);
CREATE INDEX ix_api_keys_key_prefix ON api_keys(key_prefix);

CREATE TABLE credit_buckets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    kind text NOT NULL,
    credits_total bigint NOT NULL CHECK (credits_total >= 0),
    credits_remaining bigint NOT NULL CHECK (credits_remaining >= 0),
    expires_at timestamptz,
    catalog_id text,
    source_ref text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_credit_buckets_kind CHECK (kind IN ('FREE_GRANT', 'PACK', 'SUBSCRIPTION', 'MANUAL'))
);

CREATE INDEX ix_credit_buckets_organization_remaining ON credit_buckets(organization_id, credits_remaining);
CREATE INDEX ix_credit_buckets_debit_order ON credit_buckets(organization_id, expires_at NULLS LAST, created_at);
CREATE INDEX ix_credit_buckets_subscription_rollover
    ON credit_buckets(organization_id, catalog_id, created_at)
    WHERE kind = 'SUBSCRIPTION';

CREATE TABLE credit_transactions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    bucket_id uuid REFERENCES credit_buckets(id) ON DELETE RESTRICT,
    type text NOT NULL,
    credits bigint NOT NULL,
    facet_id text,
    gtin text,
    request_id text,
    actor_user_id uuid REFERENCES users(id) ON DELETE RESTRICT,
    note text,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_credit_transactions_type CHECK (type IN ('GRANT', 'DEBIT', 'REFUND', 'EXPIRE', 'ADJUST'))
);

CREATE UNIQUE INDEX ux_credit_tx_debit_request
    ON credit_transactions(request_id, bucket_id)
    WHERE type = 'DEBIT';
CREATE INDEX ix_credit_transactions_organization_created_at
    ON credit_transactions(organization_id, created_at DESC);

CREATE TABLE stripe_customers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid UNIQUE NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    stripe_customer_id text UNIQUE NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE stripe_checkout_sessions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    stripe_session_id text UNIQUE NOT NULL,
    mode text NOT NULL,
    catalog_id text NOT NULL,
    status text NOT NULL DEFAULT 'OPEN',
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ck_stripe_checkout_sessions_mode CHECK (mode IN ('payment', 'subscription')),
    CONSTRAINT ck_stripe_checkout_sessions_status CHECK (status IN ('OPEN', 'COMPLETED', 'EXPIRED'))
);

CREATE TABLE stripe_subscriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    stripe_subscription_id text UNIQUE NOT NULL,
    catalog_id text NOT NULL,
    status text NOT NULL,
    current_period_end timestamptz,
    cancel_at timestamptz,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE invoices (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    stripe_invoice_id text UNIQUE NOT NULL,
    amount_cents integer NOT NULL,
    currency text NOT NULL DEFAULT 'eur',
    status text NOT NULL,
    hosted_invoice_url text,
    credits_granted bigint,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE stripe_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    stripe_event_id text UNIQUE NOT NULL,
    type text NOT NULL,
    processed_at timestamptz NOT NULL DEFAULT now(),
    payload jsonb
);

CREATE TABLE usage_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id uuid NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    api_key_id uuid REFERENCES api_keys(id) ON DELETE RESTRICT,
    facet_id text NOT NULL,
    gtin text,
    request_id text NOT NULL,
    http_status smallint NOT NULL,
    billable boolean NOT NULL,
    credits_consumed bigint NOT NULL DEFAULT 0,
    no_pay_reason text,
    response_time_ms integer,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_usage_events_organization_created_at ON usage_events(organization_id, created_at DESC);
CREATE INDEX ix_usage_events_facet_created_at ON usage_events(facet_id, created_at);
CREATE INDEX ix_usage_events_http_status ON usage_events(http_status);

CREATE TABLE admin_audit_events (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action text NOT NULL,
    target_organization_id uuid REFERENCES organizations(id) ON DELETE RESTRICT,
    target_ref text,
    detail jsonb,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_admin_audit_events_target_organization_created_at
    ON admin_audit_events(target_organization_id, created_at DESC);
