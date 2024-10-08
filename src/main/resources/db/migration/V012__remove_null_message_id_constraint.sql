ALTER TABLE notifications
  DROP CONSTRAINT notifications_message_id;

ALTER TABLE notifications
  ALTER COLUMN message_id DROP NOT NULL,
  ADD CONSTRAINT notifications_message_id UNIQUE NULLS NOT DISTINCT (message_id);
