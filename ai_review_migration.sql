-- AI Review Insights migration
-- Run this once against wonderlustt_db before starting the application.

ALTER TABLE review
  ADD COLUMN IF NOT EXISTS sentiment  VARCHAR(20)  NULL,
  ADD COLUMN IF NOT EXISTS ai_summary VARCHAR(255) NULL;
