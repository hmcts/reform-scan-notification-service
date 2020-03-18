package uk.gov.hmcts.reform.notificationservice.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.PENDING;

@SpringBootTest
public class NotificationRepositoryTest {

    @Autowired private NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired private NotificationRepository notificationRepository;

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM notifications", Collections.emptyMap());
    }

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
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
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
            service,
            "dcn",
            ErrorCode.ERR_AV_FAILED,
            "error_description"
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
                assertThat(n.service).isEqualTo(newNotification.service);
                assertThat(n.documentControlNumber).isEqualTo(newNotification.documentControlNumber);
                assertThat(n.errorCode).isEqualTo(newNotification.errorCode);
                assertThat(n.errorDescription).isEqualTo(newNotification.errorDescription);
                assertThat(n.createdAt).isNotNull();
                assertThat(n.processedAt).isNull();
                assertThat(n.status).isEqualTo(PENDING);
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
            service,
            "dcn1",
            ErrorCode.ERR_AV_FAILED,
            "error_description1"
        );
        final var newNotificationFromOtherService = new NewNotification(
            zipFileName,
            "po_box2",
            "other_service",
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2"
        );
        final var newNotification3 = new NewNotification(
            zipFileName,
            "po_box3",
            service,
            "dcn3",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description3"
        );

        // when
        var id1 = notificationRepository.insert(newNotification1);
        notificationRepository.insert(newNotificationFromOtherService);
        var id3 = notificationRepository.insert(newNotification3);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(2)
            .usingElementComparatorIgnoringFields("notificationId", "createdAt", "processedAt")
            .containsExactlyInAnyOrder(
                new Notification(
                    id1,
                    "someNotificationId",
                    newNotification1.zipFileName,
                    newNotification1.poBox,
                    newNotification1.service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    null,
                    PENDING
                ),
                new Notification(
                    id3,
                    "someOtherNotificationId",
                    newNotification3.zipFileName,
                    newNotification3.poBox,
                    newNotification3.service,
                    newNotification3.documentControlNumber,
                    newNotification3.errorCode,
                    newNotification3.errorDescription,
                    null,
                    null,
                    PENDING
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
            service,
            "dcn1",
            ErrorCode.ERR_AV_FAILED,
            "error_description1"
        );
        final var newNotificationForOtherZipFile = new NewNotification(
            "other_zip_file_name",
            "po_box2",
            service,
            "dcn2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "error_description2"
        );
        final var newNotification3 = new NewNotification(
            zipFileName,
            "po_box3",
            service,
            "dcn3",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description3"
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
            .usingElementComparatorIgnoringFields("notificationId", "createdAt", "processedAt")
            .containsExactlyInAnyOrder(
                new Notification(
                    id1,
                    "someNotificationId",
                    newNotification1.zipFileName,
                    newNotification1.poBox,
                    newNotification1.service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    null,
                    PENDING
                ),
                new Notification(
                    id3,
                    "someOtherNotificationId",
                    newNotification3.zipFileName,
                    newNotification3.poBox,
                    newNotification3.service,
                    newNotification3.documentControlNumber,
                    newNotification3.errorCode,
                    newNotification3.errorDescription,
                    null,
                    null,
                    PENDING
                )
            )
        ;
    }

    @Test
    void should_return_all_pending_notifications_to_be_sent_out() {
        // given
        var newNotification = createNewNotification();
        long idPending = notificationRepository.insert(newNotification);
        long idSentStillPending = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET notification_id = 'SOME_ID' WHERE id = :id",
            new MapSqlParameterSource("id", idSentStillPending)
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
                assertThat(notification.notificationId).isNull();
            });
    }

    private NewNotification createNewNotification() {
        return new NewNotification(
            "zip_file_name",
            "po_box",
            "service",
            "dcn",
            ErrorCode.ERR_AV_FAILED,
            "error_description"
        );
    }
}
