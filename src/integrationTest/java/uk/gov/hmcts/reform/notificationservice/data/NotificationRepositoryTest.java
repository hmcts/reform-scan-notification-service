package uk.gov.hmcts.reform.notificationservice.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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
    void should_return_all_pending_notifications_to_be_sent_out() {
        // given
        var newNotification = createNewNotification();
        long idPending = notificationRepository.insert(newNotification);
        long idSentStillPending = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET notification_id = 'SOME_ID' WHERE id = :id",
            new MapSqlParameterSource("id", idSentStillPending)
        );
        long idSent = notificationRepository.insert(createNewNotification());
        jdbcTemplate.update(
            "UPDATE notifications SET status = :sent WHERE id = :id",
            new MapSqlParameterSource()
                .addValue("sent", SENT)
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
