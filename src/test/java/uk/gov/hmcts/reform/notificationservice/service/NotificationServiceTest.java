package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.time.Instant;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ErrorNotificationClient notificationClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository, notificationClient);
    }

    @Test
    void should_call_repository_for_pending_notifications_to_begin_the_process() {
        notificationService.processPendingNotifications();

        verify(notificationRepository, times(1)).findPending();
        verifyNoInteractions(notificationClient);
    }

    @Test
    void should_send_error_notification_to_client_and_update_db() {
        // given
        var notification = getSampleNotification();
        var notificationId = "notification ID";
        given(notificationRepository.findPending()).willReturn(Collections.singletonList(notification));
        given(notificationClient.notify(any())).willReturn(new ErrorNotificationResponse(notificationId));

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, times(1)).markAsSent(notification.id, notificationId);
    }

    private Notification getSampleNotification() {
        return new Notification(
            1L,
            null,
            "zip_file_name",
            "po_box",
            "service",
            "DCN",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile",
            Instant.now(),
            null,
            NotificationStatus.PENDING
        );
    }
}
