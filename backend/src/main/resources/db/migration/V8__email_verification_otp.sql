CREATE TABLE IF NOT EXISTS email_verification_otps (
  id bigserial PRIMARY KEY,
  firebase_uid varchar(160) NOT NULL DEFAULT '',
  email varchar(320) NOT NULL DEFAULT '',
  otp_hash varchar(256) NOT NULL DEFAULT '',
  purpose varchar(50) NOT NULL DEFAULT '',
  expires_at timestamptz NOT NULL DEFAULT now(),
  verified_at timestamptz,
  attempts integer NOT NULL DEFAULT 0,
  resend_count integer NOT NULL DEFAULT 0,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS firebase_uid varchar(160) NOT NULL DEFAULT '';
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS email varchar(320) NOT NULL DEFAULT '';
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS otp_hash varchar(256) NOT NULL DEFAULT '';
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS purpose varchar(50) NOT NULL DEFAULT '';
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS expires_at timestamptz NOT NULL DEFAULT now();
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS verified_at timestamptz;
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS attempts integer NOT NULL DEFAULT 0;
ALTER TABLE email_verification_otps ADD COLUMN IF NOT EXISTS resend_count integer NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_email_verification_otps_uid_email
  ON email_verification_otps(firebase_uid, email);

CREATE INDEX IF NOT EXISTS idx_email_verification_otps_expires_at
  ON email_verification_otps(expires_at);

DO $$
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.table_constraints
    WHERE constraint_name = 'email_verification_otps_firebase_uid_fkey'
  ) THEN
    ALTER TABLE email_verification_otps DROP CONSTRAINT email_verification_otps_firebase_uid_fkey;
  END IF;
EXCEPTION
  WHEN OTHERS THEN NULL;
END $$;
