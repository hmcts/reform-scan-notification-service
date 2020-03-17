package uk.gov.hmcts.reform.notificationservice.data;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.notificationservice.data.notifications.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.notifications.Notification;
import uk.gov.hmcts.reform.notificationservice.data.notifications.NotificationRepository;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static uk.gov.hmcts.reform.notificationservice.data.notifications.Status.PENDING;

@ActiveProfiles({"db-test"})
@SpringBootTest
public class NotificationRepositoryTest {

    @Autowired private NotificationRepository repo;
    @Autowired private DbHelper dbHelper;

    @BeforeEach
    void setUp() {
        dbHelper.deleteAll();
    }

    @Test
    void should_save_and_read_envelope_by_id() {
        // given
        final var zipFileName = "234-567-890.zip";
        final var poBox = "123";
        final var documentControlNumber = "234-567";
        final var errorCode = "err345";
        final var errorDescription = "error descr";
        final var service = "service567";

        var newNotification = new NewNotification(
            zipFileName,
            poBox,
            documentControlNumber,
            errorCode,
            errorDescription,
            service
        );

        // when
        repo.insert(newNotification);

        // and
        List<Notification> notifications = repo.find(zipFileName, service);

        // then
        assertThat(notifications)
            .isNotEmpty()
            .hasSize(1)
            .extracting(this::getTupleFromNotification)
            .containsExactly(
                tuple(
                    zipFileName,
                    poBox,
                    documentControlNumber,
                    errorCode,
                    errorDescription,
                    null,
                    service,
                    PENDING
                )
            )
        ;
    }

    private Tuple getTupleFromNotification(Notification data) {
        return tuple(
            data.zipFileName, data.poBox, data.documentControlNumber, data.errorCode,
            data.errorDescription, data.processedAt, data.service, data.status);
    }
}
