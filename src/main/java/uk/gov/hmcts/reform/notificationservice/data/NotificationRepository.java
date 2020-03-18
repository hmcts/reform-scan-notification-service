package uk.gov.hmcts.reform.notificationservice.data;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationMapper mapper;

    public NotificationRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        NotificationMapper mapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
    }

    public Optional<Notification> find(long id) {
        try {
            Notification notification = jdbcTemplate.queryForObject(
                "SELECT * FROM notifications WHERE id = :id",
                new MapSqlParameterSource("id", id),
                this.mapper
            );

            // API suggests that it might be null
            return Optional.ofNullable(notification);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<Notification> findByStatus(NotificationStatus status) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE status = :status and notification_id IS NULL",
            new MapSqlParameterSource("status", status.name()),
            this.mapper
        );
    }

    public long insert(NewNotification notification) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            "INSERT INTO notifications (zip_file_name, po_box, service, document_control_number, "
                + "error_code, error_description, created_at, status) "
                + "VALUES ("
                + "  :zipFileName, :poBox, :service, :DCN, :errorCode, :errorDescription, CURRENT_TIMESTAMP, :status"
                + ")",
            new MapSqlParameterSource()
                .addValue("zipFileName", notification.zipFileName)
                .addValue("poBox", notification.poBox)
                .addValue("service", notification.service)
                .addValue("DCN", notification.documentControlNumber)
                .addValue("errorCode", notification.errorCode.name())
                .addValue("errorDescription", notification.errorDescription)
                .addValue("status", NotificationStatus.PENDING.name()),
            keyHolder,
            new String[]{ "id" }
        );

        return (long) keyHolder.getKey();
    }
}
