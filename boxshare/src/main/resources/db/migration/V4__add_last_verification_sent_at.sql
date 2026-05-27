ALTER TABLE users ADD COLUMN IF NOT EXISTS last_verification_sent_at TIMESTAMPTZ;
