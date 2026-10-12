-- Safe compatibility patch for fields referenced by the original PHP source but missing from the supplied dump.
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified TINYINT(1) NOT NULL DEFAULT 1;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verify_token VARCHAR(128) NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verify_expire DATETIME NULL;
