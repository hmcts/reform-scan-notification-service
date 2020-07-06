package uk.gov.hmcts.reform.notificationservice.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.FAILED;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.PENDING;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.SENT;

@Repository
public class NotificationRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final NotificationMapper mapper;
    private final int delayDurationToProcessPending;


    public NotificationRepository(
        NamedParameterJdbcTemplate jdbcTemplate,
        NotificationMapper mapper,
        @Value("${scheduling.task.pending-notifications.send-delay-in-minute}") int delayDurationToProcessPending
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.mapper = mapper;
        this.delayDurationToProcessPending = delayDurationToProcessPending;
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

    public List<Notification> find(String zipFileName, String service) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE zip_file_name = :zipFileName AND service = :service",
            new MapSqlParameterSource()
                .addValue("zipFileName", zipFileName)
                .addValue("service", service),
            this.mapper
        );
    }

    public List<Notification> findByDate(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE DATE(created_at) = :date",
            new MapSqlParameterSource("date", date),
            this.mapper
        );
    }

    public List<Notification> findPending() {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE status = :status and confirmation_id IS NULL and "
                + "created_at < (now()::timestamp - interval '" + delayDurationToProcessPending + " minutes')",
            new MapSqlParameterSource("status", PENDING.name()),
            this.mapper
        );
    }

    public long insert(NewNotification notification) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(
            "INSERT INTO notifications (zip_file_name, po_box, container, service, document_control_number, "
                + "error_code, error_description, created_at, status, message_id) "
                + "VALUES ( :zipFileName, :poBox, :container, :service, :DCN, :errorCode, "
                + ":errorDescription, CURRENT_TIMESTAMP, :status, :messageId"
                + ")",
            new MapSqlParameterSource()
                .addValue("zipFileName", notification.zipFileName)
                .addValue("poBox", notification.poBox)
                .addValue("container", notification.container)
                .addValue("service", notification.service)
                .addValue("DCN", notification.documentControlNumber)
                .addValue("errorCode", notification.errorCode.name())
                .addValue("errorDescription", notification.errorDescription)
                .addValue("status", PENDING.name())
                .addValue("messageId", notification.messageId),
            keyHolder,
            new String[]{ "id" }
        );

        return (long) keyHolder.getKey();
    }

    /**
     * Mark notification as sent.
     * @param id notification ID
     * @param confirmationId ID provided by API after successfully sending notification
     * @return update was successful
     */
    public boolean markAsSent(long id, String confirmationId) {
        int rowsUpdated = jdbcTemplate.update(
            "UPDATE notifications "
                + "SET confirmation_id = :confirmationId, "
                + "  processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("confirmationId", confirmationId)
                .addValue("status", SENT.name())
                .addValue("id", id)
        );

        return rowsUpdated == 1;
    }

    /**
     * Mark notification as failed.
     * @param id notification ID
     * @return update was successful
     */
    public boolean markAsFailure(long id) {
        int rowsUpdated = jdbcTemplate.update(
            "UPDATE notifications "
                + "SET processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("status", FAILED.name())
                .addValue("id", id)
        );

        return rowsUpdated == 1;
    }
}
