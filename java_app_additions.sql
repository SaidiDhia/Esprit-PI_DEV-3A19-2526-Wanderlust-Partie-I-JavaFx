-- Additive migration for the JavaFX app against the current Symfony-oriented schema.
-- This script only adds missing tables, columns, and compatibility views.
-- ============================================================
-- Users / security
-- ============================================================
ALTER TABLE users
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255) NULL
AFTER password;
ALTER TABLE users
MODIFY COLUMN tfa_method VARCHAR(50) NULL;
CREATE TABLE IF NOT EXISTS password_reset_tokens (
  token_id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expires_at DATETIME NOT NULL,
  INDEX idx_password_reset_tokens_user_id (user_id),
  CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
CREATE TABLE IF NOT EXISTS tfa_secrets (
  user_id VARCHAR(36) PRIMARY KEY,
  secret_key VARCHAR(255) NOT NULL,
  qr_code TEXT NULL,
  CONSTRAINT fk_tfa_secrets_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);
-- ============================================================
-- Activity logs
-- ============================================================
ALTER TABLE activity_logs
ADD COLUMN IF NOT EXISTS user_email VARCHAR(255) NULL
AFTER log_id;
-- ============================================================
-- Blog notifications
-- ============================================================
ALTER TABLE blog_notifications
ADD COLUMN IF NOT EXISTS recipient_id CHAR(36) NULL
AFTER id;
-- ============================================================
-- Booking
-- ============================================================
ALTER TABLE booking
ADD COLUMN IF NOT EXISTS host_id CHAR(36) NULL
AFTER user_id,
  ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(500) NULL
AFTER refund_amount;
-- ============================================================
-- Places compatibility layer
-- Java code uses `place` / `lat` / `lng`, while the current schema stores
-- the same entity in `places` / `latitude` / `longitude`.
-- ============================================================
CREATE OR REPLACE VIEW place AS
SELECT id,
  host_id,
  title,
  description,
  price_per_day,
  capacity,
  max_guests,
  address,
  city,
  latitude AS lat,
  longitude AS lng,
  avg_rating,
  reviews_count,
  category,
  status,
  image_url,
  denial_reason,
  created_at
FROM places;
-- ============================================================
-- Conversations / messaging
-- ============================================================
ALTER TABLE conversation
ADD COLUMN IF NOT EXISTS context_type VARCHAR(50) NULL
AFTER type,
  ADD COLUMN IF NOT EXISTS context_id BIGINT NULL
AFTER context_type,
  ADD COLUMN IF NOT EXISTS last_message_id BIGINT NULL
AFTER last_activity,
  ADD COLUMN IF NOT EXISTS mute_notifications TINYINT(1) NOT NULL DEFAULT 0
AFTER is_pinned;
-- ============================================================
-- Events
-- ============================================================
ALTER TABLE events
ADD COLUMN IF NOT EXISTS id_activite INT NULL
AFTER id,
  ADD COLUMN IF NOT EXISTS description LONGTEXT NULL
AFTER id_activite,
  ADD COLUMN IF NOT EXISTS image VARCHAR(255) NULL
AFTER materiels_necessaires,
  ADD COLUMN IF NOT EXISTS video_youtube VARCHAR(255) NULL
AFTER image,
  ADD COLUMN IF NOT EXISTS statut VARCHAR(50) NULL
AFTER status;
-- Compatibility view for the Java event photo service.
-- It maps the Java column names to the Symfony table event_images.
CREATE OR REPLACE VIEW event_photos AS
SELECT id,
  event_id AS id_event,
  image_path AS chemin_photo,
  original_name AS description,
  created_at AS date_creation
FROM event_images;
-- ============================================================
-- Notes
-- ============================================================
-- The script intentionally does not drop or rename existing Symfony columns.
-- If you later want a tighter sync between legacy and Symfony column names
-- such as `status` / `statut` or `recipient_user_id` / `recipient_id`, those
-- can be handled with a follow-up migration or triggers.