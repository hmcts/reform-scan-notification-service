package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import feign.Request;
import org.assertj.core.groups.Tuple;
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
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationResponse;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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

    @Test
    void should_return_notifications_for_file_name_and_service() {
        // given
        final String zipFileName = "zip_file_name";
        final String service = "service";

        var notification1 = new Notification(
            1L,
            "notificationId1",
            zipFileName,
            "po_box1",
            service,
            "DCN1",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile1",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT
        );
        var notification2 = new Notification(
            2L,
            "notificationId2",
            zipFileName,
            "po_box2",
            service,
            "DCN2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "invalid metafile2",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT
        );
        given(notificationRepository.find(zipFileName, service))
                  .willReturn(asList(notification1, notification2));

        // when
        var notificationResponses = notificationService.findByFileNameAndService(zipFileName, service);

        // then
        assertThat(notificationResponses)
            .hasSize(2)
            .extracting(this::getTupleFromNotificationResponse)
            .containsExactly(
                tuple(
                    notification1.notificationId,
                    notification1.zipFileName,
                    notification1.poBox,
                    notification1.service,
                    notification1.documentControlNumber,
                    notification1.errorCode.name(),
                    notification1.createdAt,
                    notification1.processedAt,
                    notification1.status.name()
                ),
                tuple(
                    notification2.notificationId,
                    notification2.zipFileName,
                    notification2.poBox,
                    notification2.service,
                    notification2.documentControlNumber,
                    notification2.errorCode.name(),
                    notification2.createdAt,
                    notification2.processedAt,
                    notification2.status.name()
                )
            );
        verify(notificationRepository, times(1)).find(zipFileName, service);
    }

    private Tuple getTupleFromNotificationResponse(NotificationResponse notificationResponse) {
        return new Tuple(
            notificationResponse.notificationId,
            notificationResponse.zipFileName,
            notificationResponse.poBox,
            notificationResponse.service,
            notificationResponse.documentControlNumber,
            notificationResponse.errorCode,
            notificationResponse.createdAt,
            notificationResponse.processedAt,
            notificationResponse.status
        );
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
