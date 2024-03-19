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

import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.FAILED;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.PENDING;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.SENT;

/**
 * The `NotificationRepository` class in Java provides methods to interact with a database table for managing
 * notifications, including finding, inserting, marking as sent or failed, and retrieving notifications based on various
 * criteria.
 */
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
     * This Java function finds a notification by its ID and returns it wrapped in an Optional, handling cases where the
     * notification may be null or not found.
     *
     * @param id The `id` parameter is the unique identifier used to search for a specific notification
     *           in the database table `notifications`. The method `find(long id)` attempts to retrieve a
     *           notification with the given `id` from the database using a SQL query.
     * @return An Optional object containing a Notification instance is being returned. If the Notification
     *      object is found in the database, it is wrapped in an Optional using Optional.ofNullable().
     *      If the Notification object is not found (resulting in an
     *      EmptyResultDataAccessException), an empty Optional is returned.
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

    /**
     * The `find` function retrieves notifications from the database based on the provided zip file name and service.
     *
     * @param zipFileName The `zipFileName` parameter is used to specify the name of the ZIP file that
     *                    is being searched for in the notifications table. The method `find` retrieves a
     *                    list of notifications that match the given `zipFileName` and `service` parameters
     *                    from the database.
     * @param service The `service` parameter in the `find` method is used to filter the notifications based
     *                on a specific service. The method retrieves notifications from the database table where
     *                the `zip_file_name` matches the provided `zipFileName` and the `service` matches
     *                the provided `service` parameter.
     * @return A List of Notification objects is being returned from the `find` method.
     */
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

    /**
     * This Java function retrieves notifications from a database based on a specified date.
     *
     * @param date The `date` parameter is of type `LocalDate` and is used to filter notifications based on
     *            the `created_at` date in the database. The `findByDate` method retrieves notifications
     *             from the database where the `created_at` date matches the provided `date` parameter.
     * @return A list of notifications that were created on the specified date.
     */
    public List<Notification> findByDate(LocalDate date) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE DATE(created_at) = :date "
                + ORDER_BY_ID,
            new MapSqlParameterSource("date", date),
            mapper
        );
    }

    /**
     * This Java function retrieves a list of notifications based on a specified zip file name using a SQL query.
     *
     * @param zipFileName The `zipFileName` parameter is used to specify the name of the ZIP file for which
     *                    you want to retrieve notifications. The `findByZipFileName` method queries the
     *                    database to find notifications that are associated with the specified `zipFileName`.
     * @return A list of `Notification` objects that match the provided `zipFileName` from the database table
     *      `notifications`.
     */
    public List<Notification> findByZipFileName(String zipFileName) {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE zip_file_name = :zipFileName "
                + ORDER_BY_ID,
            new MapSqlParameterSource(ZIP_FILE_NAME, zipFileName),
            mapper
        );
    }

    /**
     * This Java function retrieves pending notifications from a database based on certain criteria.
     *
     * @return A List of Notification objects that are pending and meet the specified criteria in the SQL query.
     */
    public List<Notification> findPending() {
        return jdbcTemplate.query(
            "SELECT * FROM notifications WHERE status = :status and confirmation_id IS NULL and "
                + "created_at < (now()::timestamp - interval '" + delayDurationToProcessPending + " minutes')",
            new MapSqlParameterSource(STATUS, PENDING.name()),
            mapper
        );
    }

    /**
     * The `insert` function inserts a new notification into a database table and returns the generated key.
     *
     * @param notification The `insert` method you provided is responsible for inserting a new notification
     *                     into a database table named `notifications`. The method takes a `NewNotification`
     *                     object as a parameter and maps its properties to the corresponding columns in the table.
     * @return The method `insert` is returning a `long` value, which is the generated key of the inserted
     *      notification record in the database.
     */
    public long insert(NewNotification notification) {
        try {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            jdbcTemplate.update(
                "INSERT INTO notifications (zip_file_name, po_box, container, service, document_control_number, "
                    + "error_code, error_description, created_at, status, message_id) "
                    + "VALUES ( :zipFileName, :poBox, :container, :service, :DCN, :errorCode, "
                    + ":errorDescription, CURRENT_TIMESTAMP, :status, :messageId"
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
                    .addValue("messageId", notification.messageId),
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
    public boolean markAsFailure(long id) {
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
}
