CREATE TABLE IF NOT EXISTS documents (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(100),
    filename     VARCHAR(500) NOT NULL,
    s3_key       VARCHAR(500) NOT NULL,
    content_type VARCHAR(200),
    file_size    BIGINT,
    status       VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
    metadata     JSONB,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_documents_category ON documents(category);
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON documents(created_at DESC);
