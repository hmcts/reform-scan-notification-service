package uk.gov.hmcts.reform.notificationservice.data;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
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
        var newNotification = new NewNotification(
            "zip_file_name",
            "po_box",
            "service",
            "dcn",
            ErrorCode.ERR_AV_FAILED,
            "error_description"
        );

        // when
        long id = notificationRepository.insert(newNotification);
        var notification = notificationRepository.find(id);

        // then
        assertThat(notification)
            .isNotEmpty()
            .get()
            .satisfies(n -> {
                assertThat(n.zipFileName).isEqualTo(newNotification.zipFileName);
                assertThat(n.poBox).isEqualTo(newNotification.poBox);
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
            .hasSize(1)
            .extracting(this::getTupleFromNotification)
            .containsExactlyInAnyOrder(
                tuple(
                    zipFileName,
                    newNotification.poBox,
                    service,
                    newNotification.documentControlNumber,
                    newNotification.errorCode,
                    newNotification.errorDescription,
                    null,
                    PENDING
                )
            )
        ;
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
        final var newNotification2 = new NewNotification(
            zipFileName,
            "po_box2",
            service,
            "dcn2",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description2"
        );

        // when
        notificationRepository.insert(newNotification1);
        notificationRepository.insert(newNotificationFromOtherService);
        notificationRepository.insert(newNotification2);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(2)
            .extracting(this::getTupleFromNotification)
            .containsExactlyInAnyOrder(
                tuple(
                    zipFileName,
                    newNotification1.poBox,
                    service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    PENDING
                ),
                tuple(
                    zipFileName,
                    newNotification2.poBox,
                    service,
                    newNotification2.documentControlNumber,
                    newNotification2.errorCode,
                    newNotification2.errorDescription,
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
        final var newNotification2 = new NewNotification(
            zipFileName,
            "po_box2",
            service,
            "dcn2",
            ErrorCode.ERR_METAFILE_INVALID,
            "error_description2"
        );

        // when
        notificationRepository.insert(newNotification1);
        notificationRepository.insert(newNotificationForOtherZipFile);
        notificationRepository.insert(newNotification2);

        // and
        List<Notification> notifications = notificationRepository.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(2)
            .extracting(this::getTupleFromNotification)
            .containsExactlyInAnyOrder(
                tuple(
                    zipFileName,
                    newNotification1.poBox,
                    service,
                    newNotification1.documentControlNumber,
                    newNotification1.errorCode,
                    newNotification1.errorDescription,
                    null,
                    PENDING
                ),
                tuple(
                    zipFileName,
                    newNotification2.poBox,
                    service,
                    newNotification2.documentControlNumber,
                    newNotification2.errorCode,
                    newNotification2.errorDescription,
                    null,
                    PENDING
                )
            )
        ;
    }

    private Tuple getTupleFromNotification(Notification data) {
        return tuple(
            data.zipFileName, data.poBox, data.service, data.documentControlNumber, data.errorCode,
            data.errorDescription, data.processedAt, data.status);
    }
}
