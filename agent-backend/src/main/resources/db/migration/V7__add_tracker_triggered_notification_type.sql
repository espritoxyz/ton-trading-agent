-- Add TRACKER_TRIGGERED value to notification_type enum
ALTER TYPE notification_type ADD VALUE IF NOT EXISTS 'TRACKER_TRIGGERED';
