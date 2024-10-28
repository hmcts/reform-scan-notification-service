package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ContextConfiguration;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClientSecondary;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.config.SecondaryClientJurisdictionsConfig;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.exception.FailedDependencyException;
import uk.gov.hmcts.reform.notificationservice.exception.NotFoundException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotifyRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = SecondaryClientJurisdictionsConfig.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ErrorNotificationClient notificationClient;

    @Mock
    private ErrorNotificationClientSecondary errorNotificationClientSecondary;

    @Mock
    private SecondaryClientJurisdictionsConfig secondaryClientJurisdictionsConfig;

    @Captor
    ArgumentCaptor<NewNotification> newNotificationCaptor;

    @Captor
    ArgumentCaptor<Long> idCaptor;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(secondaryClientJurisdictionsConfig.getJurisdictionList()).thenReturn(new String[] { "civil","cat" });
        notificationService = new NotificationService(notificationRepository,
                                                      notificationClient,
                                                      errorNotificationClientSecondary,
                                                      secondaryClientJurisdictionsConfig);
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
        var notification = getSampleNotification("primary");
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
        var notification = getSampleNotification("primary");
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
        var notification = getSampleNotification("primary");
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
        given(notificationRepository.findPending()).willReturn(singletonList(getSampleNotification("primary")));
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
        Notification notificationRetrievedFromDb = getSampleNotification("primary");
        when(notificationRepository.find(notificationRetrievedFromDb.id)).thenReturn(
            Optional.of(notificationRetrievedFromDb));

        assertThat(notificationService.findByNotificationId(Math.toIntExact(notificationRetrievedFromDb.id)))
            .isInstanceOf(NotificationInfo.class)
            .extracting("id", "confirmationId")
            .contains("12345", notificationRetrievedFromDb.confirmationId);
    }

    @Test
    void should_not_get_notification_by_id_if_it_does_not_exist() {
        int dontExist = 4342;
        String notFoundMsgPrefix = "Not found: Notification not found with ID: ";
        when(notificationRepository.find(dontExist)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.findByNotificationId(dontExist))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(notFoundMsgPrefix + dontExist);
    }

    @Test
    void should_notify_supplier_using_primary_client_when_notification_message_has_no_jurisdiction() {
        NotifyRequest notificationMsgFromMicroservice = getSampleNotificationMsgRequest(null);
        Notification savedNotificationFromdb = getSampleNotification("primary", null, NotificationStatus.CREATED);
        Notification creatednotification = getSampleNotification("primary", null, NotificationStatus.SENT);
        ErrorNotificationResponse supplierResponse = new ErrorNotificationResponse(String.valueOf(54321));

        when(notificationRepository.save(any())).thenReturn(creatednotification);
        when(notificationClient.notify(any())).thenReturn(supplierResponse);
        when(notificationRepository.updateNotificationStatusAsSent(creatednotification.id, String.valueOf(
            creatednotification.confirmationId))).thenReturn(savedNotificationFromdb);

        assertThat(notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(NotificationInfo.class)
            .extracting("id", "confirmationId")
            .contains("12345", "54321");

        verify(errorNotificationClientSecondary, never()).notify(any());
        verify(notificationRepository).save(newNotificationCaptor.capture());
        assertThat(newNotificationCaptor.getValue())
            .extracting("client")
            .isEqualTo("primary");
    }

    @Test
    void should_notify_supplier_using_secondary_client_when_notification_message_jurisdiction_is_present_in_config() {
        NotifyRequest notificationMsgFromMicroservice = getSampleNotificationMsgRequest("cat");
        Notification savedNotification = getSampleNotification("secondary", null, NotificationStatus.CREATED);
        Notification statusUpdatednotification = getSampleNotification("secondary", null, NotificationStatus.SENT);
        ErrorNotificationResponse supplierResponse = new ErrorNotificationResponse(String.valueOf(54321));

        when(notificationRepository.save(newNotificationCaptor.capture())).thenReturn(savedNotification);
        when(errorNotificationClientSecondary.notify(any())).thenReturn(supplierResponse);
        when(notificationRepository.updateNotificationStatusAsSent(statusUpdatednotification.id, String.valueOf(
            statusUpdatednotification.confirmationId))).thenReturn(savedNotification);

        assertThat(notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(NotificationInfo.class)
            .extracting("id", "confirmationId")
            .contains("12345", "54321");

        verify(notificationClient, never()).notify(any());
        verify(notificationRepository, never()).updateNotificationStatusAsFail(anyLong());
        assertThat(newNotificationCaptor.getValue())
            .extracting("client")
            .isEqualTo("secondary");
    }

    @ParameterizedTest
    @ValueSource(classes = {FeignException.BadRequest.class, FeignException.UnprocessableEntity.class,
        FeignException.class})
    void should_throw_failed_dependency_exception_when_trying_to_notify_supplier_causes_feign_exception(
        Class<FeignException> exceptionClass) {
        NotifyRequest notificationMsgFromMicroservice = getSampleNotificationMsgRequest("cat");
        Notification savedNotification = getSampleNotification("secondary", null, NotificationStatus.CREATED);
        Notification updatedStatusNotification = getSampleNotification("secondary", null, NotificationStatus.FAILED);
        FeignException mockbadrequestFeignException = mock(exceptionClass);

        when(notificationRepository.save(newNotificationCaptor.capture())).thenReturn(savedNotification);
        when(errorNotificationClientSecondary.notify(any(ErrorNotificationRequest.class))).thenThrow(
            mockbadrequestFeignException);
        when(notificationRepository.updateNotificationStatusAsFail(savedNotification.id)).thenReturn(
            updatedStatusNotification);

        assertThatThrownBy(() -> notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .isInstanceOf(FailedDependencyException.class)
            .satisfies(e -> {
                assertThat(e).extracting("notificationInfo")
                    .isInstanceOf(NotificationInfo.class)
                    .extracting("id", "confirmationId", "status")
                    .contains("12345", "54321", "FAILED");
            })
            .hasMessageContaining("The service's client failed to make a request to an external endpoint: "
                                      + "Client received status code:");

        verify(notificationRepository, never()).updateNotificationStatusAsSent(anyLong(), anyString());
        verify(notificationRepository).updateNotificationStatusAsFail(idCaptor.capture());
        assertThat(idCaptor.getValue())
            .isEqualTo(12345);
    }

    @Test
    void should_update_notification_status_to_fail_when_trying_to_notify_supplier_causes_unexpected_exception() {
        NotifyRequest notificationMsgFromMicroservice = getSampleNotificationMsgRequest("cat");
        Notification savedNotification = getSampleNotification("secondary", null, NotificationStatus.CREATED);

        when(notificationRepository.save(newNotificationCaptor.capture())).thenReturn(savedNotification);
        when(errorNotificationClientSecondary.notify(any(ErrorNotificationRequest.class)))
            .thenThrow(new RuntimeException("unexpected"));

        assertThatThrownBy(() -> notificationService.saveNotificationMsg(notificationMsgFromMicroservice))
            .hasMessage("unexpected");

        verify(notificationRepository, never()).updateNotificationStatusAsSent(anyLong(), anyString());
        verify(notificationRepository).updateNotificationStatusAsFail(idCaptor.capture());
        assertThat(idCaptor.getValue())
            .isEqualTo(12345);
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

    private NotifyRequest getSampleNotificationMsgRequest(String jurisdiction) {
        return new NotifyRequest(
            "zip_file_name",
            jurisdiction,
            "12837",
            "bulkscan",
            null,
            ErrorCode.ERR_METAFILE_INVALID,
            "Invalid metadata file.",
            "service"
        );
    }

    private Notification getSampleNotification(String client) {
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
            client
        );
    }

    private Notification getSampleNotification(String client, String messageId, NotificationStatus status) {
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
            status,
            messageId,
            client
        );
    }

    private FeignException instantiateFeignException(Class<FeignException> exceptionClass) {
        return mock(exceptionClass);
    }

    private FeignException getDefaultFeignException() {
        return mock(FeignException.FeignClientException.class);
    }

}
