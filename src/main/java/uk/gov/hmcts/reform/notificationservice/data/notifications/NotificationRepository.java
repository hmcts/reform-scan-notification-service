package uk.gov.hmcts.reform.notificationservice.data.notifications;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationMapper mapper;

    public NotificationRepository(NamedParameterJdbcTemplate jdbcTemplate, NotificationMapper mapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public List<Notification> find(String zipFileName, String service) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE zip_file_name = :zipFileName AND service = :service",
            new MapSqlParameterSource()
                .addValue("zip_file_name", zipFileName)
                .addValue("service", service),
            this.mapper
        );
    }

    public void insert(NewNotification notification) {
        jdbcTemplate.update(
            "INSERT INTO notifications (zip_file_name, po_box, document_control_number, error_code, "
                + "error_description, service, status) "
                + "VALUES (:zipFileName, :poBox, :documentControlNumber, :errorCode, "
                + ":errorDescription, :service, :status)",
            new MapSqlParameterSource()
                .addValue("zipFileName", notification.zipFileName)
                .addValue("poBox", notification.poBox)
                .addValue("documentControlNumber", notification.documentControlNumber)
                .addValue("errorCode", notification.errorCode)
                .addValue("errorDescription", notification.errorDescription)
                .addValue("service", notification.service)
                .addValue("status", Status.PENDING)
        );
    }
}
