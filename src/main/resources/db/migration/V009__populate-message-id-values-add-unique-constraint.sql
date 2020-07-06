UPDATE notifications SET message_id = CONCAT('legacy_', id)
  WHERE message_id IS NULL;

ALTER TABLE notifications
  ALTER COLUMN message_id SET NOT NULL,
  ADD CONSTRAINT notifications_message_id UNIQUE (message_id);
