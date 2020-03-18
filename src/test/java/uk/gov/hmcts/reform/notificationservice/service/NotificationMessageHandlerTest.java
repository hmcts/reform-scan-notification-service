package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
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
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationRepository notificationRepository;

    @BeforeEach
    void setUp() {
        notificationMessageHandler =
            new NotificationMessageHandler(notificationMapper, notificationRepository);
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

        NewNotification newNotification =
            new NewNotification(
                "Zipfile.zip",
                "A123",
                "processor",
                "A1342411414214",
                ErrorCode.ERR_AV_FAILED,
                "error description Av not valid"
            );

        when(notificationMapper.map(notificationMsg)).thenReturn(newNotification);
        when(notificationRepository.insert(newNotification))
            .thenReturn(21321312L);

        notificationMessageHandler.handleNotificationMessage(notificationMsg);

        // then
        verify(notificationMapper).map(notificationMsg);
        verify(notificationRepository).insert(newNotification);
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

        NewNotification newNotification =
            new NewNotification(
                "file.txt",
                "123213",
                "processor",
                "32313223",
                ErrorCode.ERR_SERVICE_DISABLED,
                "error description service disabled"
            );


        when(notificationMapper.map(notificationMsg)).thenReturn(newNotification);

        FeignException exception = mock(FeignException.class);
        doThrow(exception).when(notificationRepository).insert(newNotification);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg))
            .isSameAs(exception);

        // then
        verify(notificationMapper).map(notificationMsg);
        verify(notificationRepository).insert(newNotification);
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
        doThrow(exception).when(notificationMapper).map(notificationMsg);

        // when
        assertThatThrownBy(() -> notificationMessageHandler.handleNotificationMessage(notificationMsg))
            .isSameAs(exception);

        // then
        verify(notificationMapper).map(notificationMsg);
        verifyNoMoreInteractions(notificationRepository);
    }
}