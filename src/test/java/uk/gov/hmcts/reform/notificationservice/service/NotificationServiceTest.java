package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
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
        given(notificationRepository.findPending()).willReturn(singletonList(notification));
        given(notificationClient.notify(any())).willReturn(new ErrorNotificationResponse(notificationId));

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, times(1)).markAsSent(notification.id, notificationId);
    }

    @ParameterizedTest
    @ValueSource(classes = {FeignException.BadRequest.class, FeignException.UnprocessableEntity.class})
    void should_mark_notification_as_failed_when_relevant_exception_from_client_is_caught(
        Class<FeignException> exceptionClass
    ) {
        // given
        var notification = getSampleNotification();
        given(notificationRepository.findPending()).willReturn(singletonList(notification));
        willThrow(instantiateFeignException(exceptionClass)).given(notificationClient).notify(any());

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, times(1)).markAsFailure(notification.id);
    }

    @ParameterizedTest
    @ValueSource(classes = {FeignException.NotFound.class, FeignException.InternalServerError.class})
    void should_leave_notification_record_as_is_when_relevant_exception_from_client_is_caught(
        Class<FeignException> exceptionClass
    ) {
        // given
        var notification = getSampleNotification();
        given(notificationRepository.findPending()).willReturn(singletonList(notification));
        willThrow(instantiateFeignException(exceptionClass)).given(notificationClient).notify(any());

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, never()).markAsFailure(notification.id);
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

    @SuppressWarnings("LineLength")
    private FeignException instantiateFeignException(Class<FeignException> exceptionClass) {
        byte[] emptyByteArray = new byte[]{};

        try {
            return exceptionClass
                .getConstructor(String.class, Request.class, emptyByteArray.getClass())
                .newInstance(
                    "message",
                    Request.create(
                        Request.HttpMethod.POST,
                        "/notify",
                        emptyMap(),
                        Request.Body.create(emptyByteArray),
                        null
                    ),
                    emptyByteArray
                );
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Could not construct FeignException", e);
        }
    }
}
