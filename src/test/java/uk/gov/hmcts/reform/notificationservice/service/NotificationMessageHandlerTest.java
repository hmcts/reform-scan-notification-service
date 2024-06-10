package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.config.SecondaryClientJurisdictionsConfig;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationMessageHandlerTest {

    private NotificationMessageHandler notificationMessageHandler;
    private final String PRIMARY_CLIENT = "primary";
    private final String SECONDARY_CLIENT = "secondary";

    @Mock
    private NotificationMessageMapper notificationMessageMapper;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private SecondaryClientJurisdictionsConfig secondaryClientJurisdictionsConfig;

    @BeforeEach
    void setUp() {
        when(secondaryClientJurisdictionsConfig.getJurisdictionList()).thenReturn(new String[] { "civil","cat" });
        notificationMessageHandler =
            new NotificationMessageHandler(notificationMessageMapper,
                                           notificationRepository,
                                           secondaryClientJurisdictionsConfig);
    }

    @Test
    void should_notify_for_successful_notification_message() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "Zipfile.zip",
                "probate",
                "A123",
                "bulkscan",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid",
                "processor"
            );

        String messageId = "message12345";
        NewNotification newNotification =
            new NewNotification(
                "Zipfile.zip",
                "A123",
                "bulkscan",
                "processor",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid",
                messageId,
                PRIMARY_CLIENT
            );

        when(notificationMessageMapper.map(notificationMsg, messageId, PRIMARY_CLIENT)).thenReturn(newNotification);
        when(notificationRepository.insert(newNotification))
            .thenReturn(21321312L);

        notificationMessageHandler.handleNotificationMessage(notificationMsg, messageId);

        // then
        verify(notificationMessageMapper).map(notificationMsg, messageId, PRIMARY_CLIENT);
        verify(notificationRepository).insert(newNotification);
    }

    @Test
    void should_notify_for_successful_notification_message_for_secondary_client() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "Zipfile.zip",
                "civil",
                "A123",
                "bulkscan",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid",
                "processor"
            );

        String messageId = "message12345";
        NewNotification newNotification =
            new NewNotification(
                "Zipfile.zip",
                "A123",
                "bulkscan",
                "processor",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid",
                messageId,
                SECONDARY_CLIENT
            );

        when(notificationMessageMapper.map(notificationMsg, messageId, SECONDARY_CLIENT)).thenReturn(newNotification);
        when(notificationRepository.insert(newNotification))
            .thenReturn(21321312L);

        notificationMessageHandler.handleNotificationMessage(notificationMsg, messageId);

        // then
        verify(notificationMessageMapper).map(notificationMsg, messageId, SECONDARY_CLIENT);
        verify(notificationRepository).insert(newNotification);
    }

    @Test
    void should_rethrow_feign_exception_when_notification_call_fails() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "file.txt",
                "sscs",
                "123213",
                "reformscan",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled",
                "processor"
            );

        String messageId = "123567";
        NewNotification newNotification =
            new NewNotification(
                "file.txt",
                "123213",
                "reformscan",
                "processor",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled",
                messageId,
                PRIMARY_CLIENT
            );


        when(notificationMessageMapper.map(notificationMsg, messageId, PRIMARY_CLIENT)).thenReturn(newNotification);

        FeignException exception = mock(FeignException.class);
        doThrow(exception).when(notificationRepository).insert(newNotification);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg, messageId))
            .isSameAs(exception);

        // then
        verify(notificationMessageMapper).map(notificationMsg, messageId, PRIMARY_CLIENT);
        verify(notificationRepository).insert(newNotification);
    }

    @Test
    void should_rethrow_invalid_message_exception_when_parsing_fails() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "file.txt",
                "sscs",
                "123213",
                "scan",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled",
                "processor"
            );

        String messageId = "12345677";
        InvalidMessageException exception = new InvalidMessageException("Parsed Failed");
        doThrow(exception).when(notificationMessageMapper).map(notificationMsg, messageId, PRIMARY_CLIENT);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg, messageId))
            .isSameAs(exception);

        // then
        verify(notificationMessageMapper).map(notificationMsg, messageId, PRIMARY_CLIENT);
        verifyNoMoreInteractions(notificationRepository);
    }
}
