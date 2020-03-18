package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class NotificationMessageHandlerTest {

    private NotificationMessageHandler notificationMessageHandler;

    @Mock
    private ErrorNotificationRequestMapper errorNotificationRequestMapper;
    @Mock
    private ErrorNotificationClient errorNotificationClient;

    @BeforeEach
    void setUp() {
        notificationMessageHandler =
            new NotificationMessageHandler(errorNotificationRequestMapper, errorNotificationClient);
    }

    @Test
    public void should_notify_for_successful_notification_message() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "Zipfile.zip",
                "probate",
                "A123",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid",
                "processor"
            );

        ErrorNotificationRequest request =
            new ErrorNotificationRequest(
                "Zipfile.zip",
                "A123",
                "ERR_AV_FAILED",
                "error description Av not valid"
            );

        when(errorNotificationRequestMapper.map(notificationMsg)).thenReturn(request);
        when(errorNotificationClient.notify(request))
            .thenReturn(new ErrorNotificationResponse("312313-22311231-21321"));

        notificationMessageHandler.handleNotificationMessage(notificationMsg);

        // then
        verify(errorNotificationRequestMapper).map(notificationMsg);
        verify(errorNotificationClient).notify(request);
    }

    @Test
    public void should_rethrow_feign_exception_when_notification_call_fails() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "file.txt",
                "sscs",
                "123213",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled",
                "processor"
            );

        ErrorNotificationRequest request =
            new ErrorNotificationRequest(
                "file.txt",
                "123213",
                "ERR_SERVICE_DISABLED",
                "error description service disabled"
            );
        when(errorNotificationRequestMapper.map(notificationMsg)).thenReturn(request);

        FeignException exception = mock(FeignException.class);
        doThrow(exception).when(errorNotificationClient).notify(request);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg))
            .isSameAs(exception);

        // then
        verify(errorNotificationRequestMapper).map(notificationMsg);
        verify(errorNotificationClient).notify(request);
    }

    @Test
    public void should_rethrow_invalid_message_exception_when_parsing_fails() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "file.txt",
                "sscs",
                "123213",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled",
                "processor"
            );

        InvalidMessageException exception = new InvalidMessageException("Parsed Failed");
        doThrow(exception).when(errorNotificationRequestMapper).map(notificationMsg);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg))
            .isSameAs(exception);

        // then
        verify(errorNotificationRequestMapper).map(notificationMsg);
        verifyNoMoreInteractions(errorNotificationClient);
    }
}