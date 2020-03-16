CREATE TABLE notifications (
  id                        BIGSERIAL     PRIMARY KEY,
  notification_id           VARCHAR(50)   NULL,
  zip_file_name             VARCHAR(255)  NOT NULL,
  po_box                    VARCHAR(50)   NULL,
  service                   VARCHAR(100)  NOT NULL,
  document_control_number   VARCHAR(100)  NULL,
  error_code                VARCHAR(25)   NOT NULL,
  error_description         VARCHAR(255)  NOT NULL,
  created_at                TIMESTAMP     NOT NULL,
  processed_at              TIMESTAMP     NULL,
  status                    VARCHAR(50)   NOT NULL
);
CREATE INDEX notifications_zipfilename_service_idx ON notifications (zip_file_name, service);
