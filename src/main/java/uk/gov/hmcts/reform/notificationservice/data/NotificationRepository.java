package uk.gov.hmcts.reform.notificationservice.data;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.CREATED;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.FAILED;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.PENDING;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.SENT;

@Repository
public class NotificationRepository {

    private static final String ORDER_BY_ID = "ORDER BY id";
    private static final String ZIP_FILE_NAME = "zipFileName";
    private static final String STATUS = "status";

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

    /**
     * Uses JDBC 'SELECT' to find a notification by its ID.
     * Wraps result of query in optional to return to method caller.
     * @param id notification ID of the notification to be found
     * @return Optional of a Notification
     */
    public Optional<Notification> find(long id) {
        try {
            Notification notification = jdbcTemplate.queryForObject(
                "SELECT * FROM notifications WHERE id = :id",
                new MapSqlParameterSource("id", id),
                mapper
            );

            // API suggests that it might be null
            return Optional.ofNullable(notification);
        } catch (EmptyResultDataAccessException ex) {
            return Optional.empty();
        }
    }

    public List<Notification> find(String zipFileName, String service) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE zip_file_name = :zipFileName AND service = :service "
                + ORDER_BY_ID,
            new MapSqlParameterSource()
                .addValue(ZIP_FILE_NAME, zipFileName)
                .addValue("service", service),
            mapper
        );
    }

    public List<Notification> findByDate(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE DATE(created_at) = :date "
                + ORDER_BY_ID,
            new MapSqlParameterSource("date", date),
            mapper
        );
    }

    public List<Notification> findByZipFileName(String zipFileName) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE zip_file_name = :zipFileName "
                + ORDER_BY_ID,
            new MapSqlParameterSource(ZIP_FILE_NAME, zipFileName),
            mapper
        );
    }

    public List<Notification> findPending() { //TODO: FACT-2026
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE status = :status and confirmation_id IS NULL and "
                + "created_at < (now()::timestamp - interval '" + delayDurationToProcessPending + " minutes')",
            new MapSqlParameterSource(STATUS, PENDING.name()),
            mapper
        );
    }

    public long insert(NewNotification notification) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(
                "INSERT INTO notifications (zip_file_name, po_box, container, service, document_control_number, "
                    + "error_code, error_description, created_at, status, message_id, client) "
                    + "VALUES ( :zipFileName, :poBox, :container, :service, :DCN, :errorCode, "
                    + ":errorDescription, CURRENT_TIMESTAMP, :status, :messageId, :client"
                    + ")",
                new MapSqlParameterSource()
                    .addValue(ZIP_FILE_NAME, notification.zipFileName)
                    .addValue("poBox", notification.poBox)
                    .addValue("container", notification.container)
                    .addValue("service", notification.service)
                    .addValue("DCN", notification.documentControlNumber)
                    .addValue("errorCode", notification.errorCode.name())
                    .addValue("errorDescription", notification.errorDescription)
                    .addValue(STATUS, PENDING.name())
                    .addValue("messageId", notification.messageId)
                    .addValue("client", notification.client),
                keyHolder,
                new String[]{"id"}
            );

            return (long) keyHolder.getKey();
        } catch (DuplicateKeyException ex) {
            throw new DuplicateMessageIdException(
                String.format(
                    "Failed to save notification message for duplicate message id - %s", notification.messageId
                )
            );
        }
    }

    /**
     * Saves a notification to the notifications table.
     * Uses JDBC to first insert the notification into the table. The notification is
     * saved with an initial status of CREATED. The primary key (notification ID)
     * that is generated for the item is then captured to use in a 'SELECT' query so that the newly
     * inserted notification can be found and returned to the method caller.
     * @param notification the notification that should be saved
     * @return notification that was saved
     */
    public Notification save(NewNotification notification) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(
                "INSERT INTO notifications (zip_file_name, po_box, container, service, document_control_number, "
                    + "error_code, error_description, created_at, status, message_id, client) "
                    + "VALUES ( :zipFileName, :poBox, :container, :service, :DCN, :errorCode, "
                    + ":errorDescription, CURRENT_TIMESTAMP, :status, :messageId, :client"
                    + ")",
                new MapSqlParameterSource()
                    .addValue(ZIP_FILE_NAME, notification.zipFileName)
                    .addValue("poBox", notification.poBox)
                    .addValue("container", notification.container)
                    .addValue("service", notification.service)
                    .addValue("DCN", notification.documentControlNumber)
                    .addValue("errorCode", notification.errorCode.name())
                    .addValue("errorDescription", notification.errorDescription)
                    .addValue(STATUS, CREATED.name())
                    .addValue("messageId", notification.messageId)
                    .addValue("client", notification.client),
                keyHolder,
                new String[]{"id"}
            );

            return jdbcTemplate.queryForObject(
                "SELECT * FROM notifications WHERE id = :id",
                new MapSqlParameterSource("id", keyHolder.getKey()),
                mapper
            );

        } catch (DuplicateKeyException ex) {
            throw new DuplicateMessageIdException(
                String.format(
                    "Failed to save notification message for duplicate message id - %s", notification.messageId
                )
            );
        }
    }

    /**
     * Mark notification as sent.
     * @param id notification ID
     * @param confirmationId ID provided by API after successfully sending notification
     * @return update was successful
     */
    public boolean markAsSent(long id, String confirmationId) { //TODO: FACT-2026
        int rowsUpdated = jdbcTemplate.update(
            "UPDATE notifications "
                + "SET confirmation_id = :confirmationId, "
                + "  processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("confirmationId", confirmationId)
                .addValue(STATUS, SENT.name())
                .addValue("id", id)
        );

        return rowsUpdated == 1;
    }

    /**
     * Mark notification as failed.
     *
     * @param id notification ID
     * @return update was successful
     */
    public boolean markAsFailure(long id) { //TODO: FACT-2026
        int rowsUpdated = jdbcTemplate.update(
            "UPDATE notifications "
                + "SET processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue(STATUS, FAILED.name())
                .addValue("id", id)
        );

        return rowsUpdated == 1;
    }

    /**
     * Updates the status column of a notification row in the Notifications table
     * to have the status of FAILED.
     * Uses JDBC update to set the processed at column to now and the status column to FAILED.
     * It then uses a 'SELECT' to find the updated notification and return to the method caller.
     * @param notificationId the ID of the notification whose status should be updated
     * @return notification that was updated
     */
    public Notification updateNotificationStatusAsFail(long notificationId) {
        jdbcTemplate.update(
            "UPDATE notifications "
                + "SET processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue(STATUS, FAILED.name())
                .addValue("id", notificationId)
        );

        return jdbcTemplate.queryForObject(
            "SELECT * FROM notifications WHERE id = :id",
            new MapSqlParameterSource("id", notificationId),
            mapper
        );
    }

    /**
     * Updates the status column of a notification row in the Notifications table
     * to have the status of SENT.
     * Uses JDBC update to set the processed at column to now, the status column to SENT
     * and the confirmation ID (supplier ID) column to the given ID.
     * It then uses a 'SELECT' to find the notification that was updated to return to method caller
     * @param notificationId the ID of the notification that should be updated
     * @param confirmationId the ID returned by the supplier when it was notified
     * @return notification that was updated
     */
    public Notification updateNotificationStatusAsSent(long notificationId, String confirmationId) {
        jdbcTemplate.update(
            "UPDATE notifications "
                + "SET confirmation_id = :confirmationId, "
                + "  processed_at = NOW(), "
                + "  status = :status "
                + "WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("confirmationId", confirmationId)
                .addValue(STATUS, SENT.name())
                .addValue("id", notificationId)
        );

        return jdbcTemplate.queryForObject(
            "SELECT * FROM notifications WHERE id = :id",
            new MapSqlParameterSource("id", notificationId),
            mapper
        );
    }
}
