package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void should_call_repository_for_pending_notifications_to_begin_the_process() {
        getNotificationService().processPendingNotifications();

        verify(notificationRepository, times(1)).findPending();
    }

    private NotificationService getNotificationService() {
        return new NotificationService(notificationRepository);
    }
}
