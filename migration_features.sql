-- ============================================================
-- Migration: Features — Geolocation, PDF Upload, Reviews
-- Run this on your `wonderlustt_db` database before launching the app
-- ============================================================

-- 1) Geolocation columns on place
ALTER TABLE place ADD COLUMN IF NOT EXISTS lat DOUBLE NULL;
ALTER TABLE place ADD COLUMN IF NOT EXISTS lng DOUBLE NULL;

-- 2) Rating aggregation columns on place (optional but recommended for performance)
ALTER TABLE place ADD COLUMN IF NOT EXISTS avg_rating DOUBLE NULL;
ALTER TABLE place ADD COLUMN IF NOT EXISTS reviews_count INT NULL DEFAULT 0;

-- 3) PDF path column on booking
ALTER TABLE booking ADD COLUMN IF NOT EXISTS pdf_path VARCHAR(500) NULL;

-- 4) Review table
CREATE TABLE IF NOT EXISTS review (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  place_id   INT NOT NULL,
  user_id    INT NOT NULL,
  rating     INT NOT NULL,
  comment    TEXT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(place_id, user_id),
  FOREIGN KEY (place_id) REFERENCES place(id) ON DELETE CASCADE
);
