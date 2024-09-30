package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClientSecondary;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.exception.BadRequestException;
import uk.gov.hmcts.reform.notificationservice.exception.NotFoundException;
import uk.gov.hmcts.reform.notificationservice.exception.UnprocessableEntityException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ErrorNotificationClient notificationClient;

    @Mock
    private ErrorNotificationClientSecondary errorNotificationClientSecondary;

    @Mock
    private NotificationMessageMapper notificationMessageMapper;

    @Mock
    private ErrorNotificationClient errorNotificationClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(notificationRepository,
                                                      notificationClient,
                                                      errorNotificationClientSecondary);
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
    @ValueSource(classes = {
        FeignException.class,
        FeignException.NotFound.class,
        FeignException.InternalServerError.class
    })
    void should_leave_notification_record_as_is_when_relevant_exception_from_client_is_caught(
        Class<FeignException> exceptionClass
    ) {
        // given
        var notification = getSampleNotification();
        var exception = exceptionClass.equals(FeignException.class)
            ? getDefaultFeignException()
            : instantiateFeignException(exceptionClass);
        given(notificationRepository.findPending()).willReturn(singletonList(notification));
        willThrow(exception).given(notificationClient).notify(any());

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, never()).markAsFailure(notification.id);
    }

    @Test
    void should_leave_notification_as_is_when_unexpected_exception_is_thrown_and_continue() {
        // given
        given(notificationRepository.findPending()).willReturn(singletonList(getSampleNotification()));
        willThrow(new RuntimeException()).given(notificationClient).notify(any());

        // when
        notificationService.processPendingNotifications();

        // then
        verify(notificationRepository, never()).markAsFailure(anyLong());
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
            "bulkscan",
            service,
            "DCN1",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile1",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT,
            "messageId1",
            "primary"
        );
        var notification2 = new Notification(
            2L,
            "notificationId2",
            zipFileName,
            "po_box2",
            "reformscan",
            service,
            "DCN2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "invalid metafile2",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT,
            "messageId2",
            "primary"
        );
        given(notificationRepository.find(zipFileName, service))
                  .willReturn(asList(notification1, notification2));

        // when
        var notificationResponses = notificationService.findByFileNameAndService(zipFileName, service);

        // then
        assertThat(notificationResponses)
            .hasSize(2)
            .extracting(this::getTupleFromNotification)
            .containsExactlyInAnyOrder(
                tuple(
                    notification1.confirmationId,
                    notification1.zipFileName,
                    notification1.poBox,
                    notification1.service,
                    notification1.documentControlNumber,
                    notification1.errorCode,
                    notification1.createdAt,
                    notification1.processedAt,
                    notification1.status,
                    notification1.messageId
                ),
                tuple(
                    notification2.confirmationId,
                    notification2.zipFileName,
                    notification2.poBox,
                    notification2.service,
                    notification2.documentControlNumber,
                    notification2.errorCode,
                    notification2.createdAt,
                    notification2.processedAt,
                    notification2.status,
                    notification2.messageId
                )
            );
        verify(notificationRepository, times(1)).find(zipFileName, service);
    }

    @Test
    void should_return_notifications_for_date() {
        // given
        LocalDate searchDate = LocalDate.now();

        var notification1 = new Notification(
            1L,
            "notification_id_1",
            "zip_file_name_12",
            "po_box_1",
            "bulk_scan",
            "service_1",
            "DCN_1",
            ErrorCode.ERR_AV_FAILED,
            "invalid metafile_1",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT,
            "messageId1",
            "primary"
        );
        var notification2 = new Notification(
            2L,
            "notification_id_2",
            "zip_file_23234.zip",
            "po_box_2",
            "reform_scan",
            "service_99",
            "DCN_2",
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "invalid metafile_2",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT,
            "messageId1",
            "primary"
        );
        given(notificationRepository.findByDate(searchDate))
            .willReturn(asList(notification1, notification2));

        // when
        var notificationResponses = notificationService.findByDate(searchDate);

        // then
        assertThat(notificationResponses)
            .hasSize(2)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(notification1, notification2);

        verify(notificationRepository, times(1)).findByDate(searchDate);
    }

    @Test
    void should_return_notifications_for_zip_file_name() {
        // given
        String zipFileName = "zip_file_name_12";
        var notification1 = new Notification(
            1L,
            "notification_id_1",
            zipFileName,
            "po_box_1",
            "bulk_scan",
            "service_1",
            "DCN_1",
            ErrorCode.ERR_AV_FAILED,
            "invalid metafile_1",
            Instant.now(),
            Instant.now(),
            NotificationStatus.SENT,
            "messageId1",
            "primary"
        );
        given(notificationRepository.findByZipFileName(zipFileName))
            .willReturn(singletonList(notification1));

        // when
        var notificationResponses = notificationService.findByZipFileName(zipFileName);

        // then
        assertThat(notificationResponses)
            .hasSize(1)
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactly(notification1);
    }

    @Test
    void should_call_repository_for_all_pending_notifications() {
        // given
        Notification notification1 = mock(Notification.class);
        Notification notification2 = mock(Notification.class);
        List<Notification> notifications = asList(notification1,notification2);
        given(notificationRepository.findPending()).willReturn(notifications);

        // when
        List<Notification> res = notificationService.getAllPendingNotifications();

        // then
        assertThat(res).isSameAs(notifications);
    }

    @Test
    void should_get_notification_by_id() {
        Notification notificationRetrievedFromDb = getSampleNotification();
        when(notificationRepository.find(notificationRetrievedFromDb.id)).thenReturn(Optional.of(notificationRetrievedFromDb));

        assertThat(notificationService.findByNotificationId(Math.toIntExact(notificationRetrievedFromDb.id)))
            .isInstanceOf(NotificationInfo.class)
            .extracting("id", "confirmationId")
            .contains(notificationRetrievedFromDb.id, notificationRetrievedFromDb.confirmationId);

        verify(notificationRepository, times(1)).find(any());
    }

    @Test
    void should_not_get_notification_by_id_if_it_does_not_exist() {
        int iDontExistId = 4342;
        String notFoundMsgPrefix = "Not found: Notification not found with ID: ";
        when(notificationRepository.find(iDontExistId)).thenReturn(null);

        assertThatThrownBy(() -> notificationService.findByNotificationId(iDontExistId))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(notFoundMsgPrefix + iDontExistId);

        verify(notificationRepository, times(1)).find(any());
    }

    //FIXME Should probably be able to make the notification service do one call to to the DB with refactoring
    @Test
    void should_save_a_notification_message_to_db() {
        NotificationMsg notificationMsgFromMicroservice = getSampleNotificationMsg();
        NewNotification msgMappedToNotification = getSampleNewNotification();
        Notification savedNotificationFromdb = getSampleNotification();

        when(notificationMessageMapper.map(notificationMsgFromMicroservice, null, "primary")).thenReturn(msgMappedToNotification);
        when(notificationRepository.insert(msgMappedToNotification)).thenReturn(1L);
        when(errorNotificationClient.notify(new ErrorNotificationRequest(
            savedNotificationFromdb.zipFileName, savedNotificationFromdb.poBox, savedNotificationFromdb.errorCode.toString(),
            savedNotificationFromdb.errorDescription)));
        when(notificationRepository.markAsSent(1L, "54321")).thenReturn(true);
        when(notificationRepository.find(1L)).thenReturn(Optional.of(savedNotificationFromdb));

        assertThat(notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(NotificationInfo.class)
            .extracting("id", "confirmation_id")
            .contains("1", "54321");
    }

    //TODO: Update with Exela Bad Request message
    @Test
    void should_throw_exception_when_supplier_returns_bad_request_after_being_notified() {
        NotificationMsg notificationMsgFromMicroservice = getSampleNotificationMsg();
        NewNotification msgMappedToNotification = getSampleNewNotification();
        Notification savedNotificationFromdb = getSampleNotification();
        String badRequestMsgPrefix = "A BadRequestException was received from supplier. Notification has been marked as a fail. Notification ID: ";

        when(notificationMessageMapper.map(notificationMsgFromMicroservice, null, "primary")).thenReturn(msgMappedToNotification);
        when(notificationRepository.insert(msgMappedToNotification)).thenReturn(1L);
        when(errorNotificationClient.notify(new ErrorNotificationRequest(
            savedNotificationFromdb.zipFileName, savedNotificationFromdb.poBox, savedNotificationFromdb.errorCode.toString(),
            savedNotificationFromdb.errorDescription))).thenThrow(new FeignException.BadRequest(
                "Supplier could not save notification",
                null, null, null ));

        assertThatThrownBy(() -> notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(BadRequestException.class)
            .hasMessage(badRequestMsgPrefix + 1L);
    }

    //TODO: Need to check Exela do this and update controller/tests respectively
    //TODO: Update with Exela message
    @Test
    void should_throw_exception_when_supplier_returns_unprocessable_entity_after_being_notified() {
        NotificationMsg notificationMsgFromMicroservice = getSampleNotificationMsg();
        NewNotification msgMappedToNotification = getSampleNewNotification();
        Notification savedNotificationFromdb = getSampleNotification();
        String UnprocessableEntityMsgPrefix = "Supplier was unable to process the notification request. Notification ID: ";

        when(notificationMessageMapper.map(notificationMsgFromMicroservice, null, "primary")).thenReturn(msgMappedToNotification);
        when(notificationRepository.insert(msgMappedToNotification)).thenReturn(1L);
        when(errorNotificationClient.notify(new ErrorNotificationRequest(
            savedNotificationFromdb.zipFileName, savedNotificationFromdb.poBox, savedNotificationFromdb.errorCode.toString(),
            savedNotificationFromdb.errorDescription))).thenThrow(new FeignException.UnprocessableEntity(
            "Supplier could not save notification",
            null, null, null ));

        assertThatThrownBy(() -> notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(UnprocessableEntityException.class)
            .hasMessage(UnprocessableEntityMsgPrefix + 1L);
    }

    private Tuple getTupleFromNotification(Notification notification) {
        return new Tuple(
            notification.confirmationId,
            notification.zipFileName,
            notification.poBox,
            notification.service,
            notification.documentControlNumber,
            notification.errorCode,
            notification.createdAt,
            notification.processedAt,
            notification.status,
            notification.messageId
        );
    }

    private NewNotification getSampleNewNotification() {
        return new NewNotification(
            "zip_file_name",
            "12837",
            "bulkscan",
            "nfd",
            null,
            ErrorCode.ERR_METAFILE_INVALID,
            "Invalid metadata file.",
            null,
            "primary"
        );
    }
    private NotificationMsg getSampleNotificationMsg() {
        return new NotificationMsg(
            "zip_file_name",
            null,
            "12837",
            "bulkscan",
            null,
            ErrorCode.ERR_METAFILE_INVALID,
            "Invalid metadata file.",
            "nfd"
        );
    }
    private Notification getSampleNotification() {
        return new Notification(
            12345,
            "54321",
            "zip_file_name",
            "po_box",
            "bulkscan",
            "service",
            "DCN",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile",
            Instant.now(),
            null,
            NotificationStatus.PENDING,
            "messageId1",
            "primary"
        );
    }

    private FeignException instantiateFeignException(Class<FeignException> exceptionClass) {
        return mock(exceptionClass);
    }

    private FeignException getDefaultFeignException() {
        return mock(FeignException.FeignClientException.class);
    }

}
