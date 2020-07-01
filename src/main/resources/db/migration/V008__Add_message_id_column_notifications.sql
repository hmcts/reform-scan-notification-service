ALTER TABLE notifications
  ADD COLUMN message_id VARCHAR(255) NULL,
  ADD CONSTRAINT notifications_message_id UNIQUE (message_id);
