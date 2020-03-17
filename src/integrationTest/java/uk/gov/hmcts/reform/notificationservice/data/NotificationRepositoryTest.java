package uk.gov.hmcts.reform.notificationservice.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

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
                assertThat(n.status).isEqualTo(NotificationStatus.PENDING);
            });
    }

    @Test
    void should_return_empty_optional_when_there_is_no_notification_in_db() {
        assertThat(notificationRepository.find(1_000)).isEmpty();
    }
}
