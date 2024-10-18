package uk.gov.hmcts.reform.notificationservice.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.FAILED;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.PENDING;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.SENT;

@SpringBootTest
public class NotificationRepositoryTest {

    @Autowired NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired NotificationRepository notificationRepository;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notifications", Collections.emptyMap());
    }

    private static final String PRIMARY_CLIENT = "primary";

    @Test
    void should_save_and_read_notification() {
        // given
        var newNotification = createNewNotification();
        long id = notificationRepository.insert(newNotification);

        // when
        var notification = notificationRepository.find(id);

        // then
        assertThat(notification)
            .isNotEmpty()
            .get()
            .satisfies(n -> {
                assertThat(n.zipFileName).isEqualTo(newNotification.zipFileName);
                assertThat(n.poBox).isEqualTo(newNotification.poBox);
                assertThat(n.container).isEqualTo(newNotification.container);
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
                assertThat(n.messageId).isEqualTo(newNotification.messageId);
            });
    }

    @Test
    void should_return_empty_optional_when_there_is_no_notification_in_db() {
        assertThat(notificationRepository.find(1_000)).isEmpty();
    }

    @Test
    void should_find_single_notification_by_zip_file_name_and_service() {
        // given
        final var zipFileName = "zip_file_name";
        final var service = "service";

        final var newNotification = new NewNotification(
            zipFileName,
            "po_box",
            "bulkscan",
            service,
            "dcn",
            ErrorCode.ERR_AV_FAILED,
            "error_description",
            "123456",
            PRIMARY_CLIENT
        );

        // when
        notificationRepository.insert(newNotification);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(1);
        assertThat(notifications.get(0))
            .satisfies(n -> {
                assertThat(n.zipFileName).isEqualTo(newNotification.zipFileName);
                assertThat(n.poBox).isEqualTo(newNotification.poBox);
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.container).isEqualTo(newNotification.container);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
                assertThat(n.messageId).isEqualTo(newNotification.messageId);
                assertThat(n.client).isEqualTo(PRIMARY_CLIENT);
            });
    }

    @Test
    void should_exclude_notifications_from_other_service() {
        // given
        final var zipFileName = "zip_file_name";
        final var service = "service";

        final var newNotification1 = new NewNotification(
            zipFileName,
            "po_box1",
            "reformscan",
            service,
            "dcn1",
            ErrorCode.ERR_AV_FAILED,
            "error_description1",
            "12345",
            PRIMARY_CLIENT
        );
        final var newNotificationFromOtherService = new NewNotification(
            zipFileName,
            "po_box2",
            "bulkscan",
            "other_service",
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2",
            "1234445",
            PRIMARY_CLIENT
        );
        final var newNotification3 = new NewNotification(
            zipFileName,
            "po_box3",
            "scan",
            service,
            "dcn3",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description3",
            "124355676",
            PRIMARY_CLIENT
        );

        final var newNotification4 = new NewNotification(
            zipFileName,
            "po_box4",
            "scan",
            service,
            "dcn4",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description4",
            "124355666",
            PRIMARY_CLIENT
        );

        // when
        var id4 = notificationRepository.insert(newNotification4);
        var id1 = notificationRepository.insert(newNotification1);
        notificationRepository.insert(newNotificationFromOtherService);
        var id3 = notificationRepository.insert(newNotification3);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(3)
            .usingElementComparatorIgnoringFields("createdAt", "processedAt")
            .containsExactly(
                new Notification(
                    id4,
                    null,
                    newNotification4.zipFileName,
                    newNotification4.poBox,
                    newNotification4.container,
                    newNotification4.service,
                    newNotification4.documentControlNumber,
                    newNotification4.errorCode,
                    newNotification4.errorDescription,
                    null,
                    null,
                    PENDING,
                    newNotification4.messageId,
                    PRIMARY_CLIENT
                ),
                new Notification(
                    id1,
                    null,
                    newNotification1.zipFileName,
                    newNotification1.poBox,
                    newNotification1.container,
                    newNotification1.service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    null,
                    PENDING,
                    newNotification1.messageId,
                    PRIMARY_CLIENT
                ),
                new Notification(
                    id3,
                    null,
                    newNotification3.zipFileName,
                    newNotification3.poBox,
                    newNotification3.container,
                    newNotification3.service,
                    newNotification3.documentControlNumber,
                    newNotification3.errorCode,
                    newNotification3.errorDescription,
                    null,
                    null,
                    PENDING,
                    newNotification3.messageId,
                    PRIMARY_CLIENT
                )
            )
        ;
    }

    @Test
    void should_exclude_notifications_for_other_zip_file() {
        // given
        final var zipFileName = "zip_file_name";
        final var service = "service";

        final var newNotification1 = new NewNotification(
            zipFileName,
            "po_box1",
            "bulkscan",
            service,
            "dcn1",
            ErrorCode.ERR_AV_FAILED,
            "error_description1",
            "1234555",
            PRIMARY_CLIENT
        );
        final var newNotificationForOtherZipFile = new NewNotification(
            "other_zip_file_name",
            "po_box2",
            "reformscan",
            service,
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2",
            "12121331",
            PRIMARY_CLIENT
        );
        final var newNotification3 = new NewNotification(
            zipFileName,
            "po_box3",
            "scan",
            service,
            "dcn3",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description3",
            "54321",
            PRIMARY_CLIENT
        );

        // when
        var id1 = notificationRepository.insert(newNotification1);
        notificationRepository.insert(newNotificationForOtherZipFile);
        var id3 = notificationRepository.insert(newNotification3);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(2)
            .usingElementComparatorIgnoringFields("createdAt", "processedAt")
            .containsExactly(
                new Notification(
                    id1,
                    null,
                    newNotification1.zipFileName,
                    newNotification1.poBox,
                    newNotification1.container,
                    newNotification1.service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    null,
                    PENDING,
                    newNotification1.messageId,
                    PRIMARY_CLIENT
                ),
                new Notification(
                    id3,
                    null,
                    newNotification3.zipFileName,
                    newNotification3.poBox,
                    newNotification3.container,
                    newNotification3.service,
                    newNotification3.documentControlNumber,
                    newNotification3.errorCode,
                    newNotification3.errorDescription,
                    null,
                    null,
                    PENDING,
                    newNotification3.messageId,
                    PRIMARY_CLIENT
                )
            )
        ;
    }

    @Test
    void should_return_all_pending_notifications_to_be_sent_out() {
        // given
        var newNotification = createNewNotification();
        // valid pending
        long idPending = notificationRepository.insert(newNotification);
        jdbcTemplate.update(
            "UPDATE notifications SET created_at = (now()::timestamp - interval '65 minutes')",
            new MapSqlParameterSource("id", idPending)
        );

        // record should wait 2 hour before it is picked up
        notificationRepository.insert(createNewNotification());

        // confirmation_id should be null to be picked up
        long idSentStillPending = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET confirmation_id = 'SOME_ID' WHERE id = :id",
            new MapSqlParameterSource("id", idSentStillPending)
        );
        long idSent = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET status = :sent WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("sent", SENT.name())
                .addValue("id", idSent)
        );

        // when
        var notifications = notificationRepository.findPending();

        // then
        assertThat(notifications)
            .hasSize(1)
            .first()
            .satisfies(notification -> {
                assertThat(notification.id).isEqualTo(idPending);
                assertThat(notification.status).isEqualTo(PENDING);
                assertThat(notification.messageId).isEqualTo(newNotification.messageId);
                assertThat(notification.confirmationId).isNull();
            });
    }

    @Test
    void should_return_flag_false_when_mark_as_sent_did_not_find_any_notification_to_update() {
        // when
        boolean isMarked = notificationRepository.markAsSent(1_000, "foo");

        // then
        assertThat(isMarked).isFalse();
    }

    @Test
    void should_return_flag_true_when_mark_as_sent_was_successful() {
        // given
        long id = notificationRepository.insert(createNewNotification());
        String confirmationId = "aisjdaoisd";

        // when
        boolean isMarked = notificationRepository.markAsSent(id, confirmationId);

        // then
        assertThat(isMarked).isTrue();

        // and
        assertThat(notificationRepository.find(id))
            .isNotEmpty()
            .get()
            .satisfies(notification -> {
                assertThat(notification.confirmationId).isEqualTo(confirmationId);
                assertThat(notification.status).isEqualTo(SENT);
                assertThat(notification.processedAt).isNotNull();
            });
    }

    @Test
    void should_return_flag_false_when_mark_as_failure_did_not_find_any_notification_to_update() {
        // when
        boolean isMarked = notificationRepository.markAsFailure(1_000);

        // then
        assertThat(isMarked).isFalse();
    }

    @Test
    void should_return_flag_true_when_mark_as_failure_was_successful() {
        // given
        long id = notificationRepository.insert(createNewNotification());

        // when
        boolean isMarked = notificationRepository.markAsFailure(id);

        // then
        assertThat(isMarked).isTrue();

        // and
        assertThat(notificationRepository.find(id))
            .isNotEmpty()
            .get()
            .satisfies(notification -> {
                assertThat(notification.status).isEqualTo(FAILED);
                assertThat(notification.processedAt).isNotNull();
            });
    }

    @Test
    void should_find_notification_by_date() {
        // given
        final var newNotification = new NewNotification(
            "zip_file_123213.zip",
            "po_box2",
            "bulkscan",
            "other_service",
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2",
            "123455",
            PRIMARY_CLIENT
        );
        notificationRepository.insert(newNotification);

        long idDifferentDate = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET created_at ='" + LocalDate.now().minusDays(2) + "' WHERE id = :id",
            new MapSqlParameterSource("id", idDifferentDate)
        );

        // when
        List<Notification> notifications = notificationRepository.findByDate(LocalDate.now());

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(1);
        assertThat(notifications.get(0))
            .satisfies(n -> {
                assertThat(n.zipFileName).isEqualTo(newNotification.zipFileName);
                assertThat(n.poBox).isEqualTo(newNotification.poBox);
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.container).isEqualTo(newNotification.container);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
                assertThat(n.messageId).isEqualTo(newNotification.messageId);
                assertThat(n.client).isEqualTo(PRIMARY_CLIENT);
            });
    }

    @Test
    void should_find_notification_by_zip_file_name() {
        // given
        final var newNotification = new NewNotification(
            "zip_file_123213.zip",
            "po_box2",
            "bulkscan",
            "other_service",
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2",
            "123455",
            PRIMARY_CLIENT
        );
        notificationRepository.insert(newNotification);

        // when
        List<Notification> notifications = notificationRepository.findByZipFileName("zip_file_123213.zip");

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(1);
        assertThat(notifications.get(0))
            .satisfies(n -> {
                assertThat(n.zipFileName).isEqualTo(newNotification.zipFileName);
                assertThat(n.poBox).isEqualTo(newNotification.poBox);
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.container).isEqualTo(newNotification.container);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
                assertThat(n.messageId).isEqualTo(newNotification.messageId);
                assertThat(n.client).isEqualTo(PRIMARY_CLIENT);
            });
    }

    @Test
    @Disabled
    void should_throw_exception_for_duplicate_message_id() {
        // given
        String messageId = UUID.randomUUID().toString();
        final var newNotification1 = new NewNotification(
            "zip_file_1.zip",
            "po_box1",
            "bulkscan",
            "other_service",
            "dcn1",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description1",
            messageId,
            PRIMARY_CLIENT
        );
        final var newNotification2 = new NewNotification(
            "zip_file_2.zip",
            "po_box2",
            "bulkscan",
            "other_service",
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2",
            messageId,
            PRIMARY_CLIENT
        );

        // when
        notificationRepository.insert(newNotification1);
        Throwable exception = catchThrowable(() -> notificationRepository.insert(newNotification2));

        // then
        assertThat(exception)
            .isInstanceOf(DuplicateMessageIdException.class)
            .hasMessage("Failed to save notification message for duplicate message id - " + messageId);
    }

    private NewNotification createNewNotification() {
        return new NewNotification(
            "zip_file_name",
            "po_box",
            "bulkscan",
            "service",
            "dcn",
            ErrorCode.ERR_AV_FAILED,
            "error_description",
            UUID.randomUUID().toString(),
            PRIMARY_CLIENT
        );
    }
}
