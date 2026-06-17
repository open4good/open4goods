CREATE TABLE barcode_assets (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    token text UNIQUE NOT NULL,
    content bytea NOT NULL,
    content_type text NOT NULL,
    expires_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX ix_barcode_assets_token ON barcode_assets(token);
CREATE INDEX ix_barcode_assets_expires_at ON barcode_assets(expires_at);
