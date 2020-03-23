package uk.gov.hmcts.reform.notificationservice.task;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PendingNotificationsTaskTest {

    @Mock
    private NotificationService notificationService;

    @Test
    void should_call_service_once() {
        new PendingNotificationsTask(notificationService).run();

        verify(notificationService, times(1)).processPendingNotifications();
    }
}
