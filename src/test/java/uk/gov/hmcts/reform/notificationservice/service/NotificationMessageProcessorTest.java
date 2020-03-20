package uk.gov.hmcts.reform.notificationservice.service;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.MessageBody;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class NotificationMessageProcessorTest {

    private NotificationMessageProcessor notificationMessageProcessor;

    @Mock
    private IMessageReceiver messageReceiver;
    @Mock
    private NotificationMessageHandler notificationMessageHandler;
    @Mock
    private NotificationMessageParser notificationMessageParser;
    @Mock
    private IMessage message;
    @Mock
    private MessageBody messageBody;

    @BeforeEach
    public void before() throws Exception {
        notificationMessageProcessor = new NotificationMessageProcessor(
            messageReceiver,
            notificationMessageHandler,
            notificationMessageParser,
            5
        );
    }

    @Test
    public void should_return_true_when_there_is_a_message_to_process() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);
        given(messageReceiver.receive()).willReturn(message);

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);
        doNothing().when(notificationMessageHandler).handleNotificationMessage(notificationMsg);

        // when
        boolean processedMessage = notificationMessageProcessor.processNextMessage();


        // then
        assertThat(processedMessage).isTrue();
        verify(messageReceiver).receive();
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg);

    }

    @Test
    public void should_return_false_when_there_is_no_message_to_process() throws Exception {
        // given
        given(messageReceiver.receive()).willReturn(null);

        // when
        boolean processedMessage = notificationMessageProcessor.processNextMessage();

        // then
        assertThat(processedMessage).isFalse();
        verifyNoMoreInteractions(notificationMessageParser);
        verifyNoMoreInteractions(notificationMessageHandler);
    }

    @Test
    public void should_not_throw_exception_when_queue_message_is_invalid() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);
        given(messageReceiver.receive()).willReturn(message);
        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("Invalid Message"));

        //when
        assertThat(notificationMessageProcessor.processNextMessage()).isTrue();

        // then
        verify(messageReceiver).receive();
        verify(notificationMessageParser).parse(messageBody);
        verifyNoMoreInteractions(notificationMessageHandler);
    }

    @Test
    public void should_not_throw_exception_when_notification_handler_fails() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);
        given(messageReceiver.receive()).willReturn(message);

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);
        willThrow(new RuntimeException("Handle exception"))
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg);

        //when
        assertThatCode(() -> notificationMessageProcessor.processNextMessage()).doesNotThrowAnyException();

        // then
        verify(messageReceiver).receive();
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg);
    }

    @Test
    public void should_complete_the_message_when_processing_is_successful() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);
        UUID lock = UUID.randomUUID();
        given(message.getLockToken()).willReturn(lock);

        given(messageReceiver.receive()).willReturn(message);

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);
        doNothing().when(notificationMessageHandler).handleNotificationMessage(notificationMsg);

        // when
        notificationMessageProcessor.processNextMessage();


        // then
        verify(messageReceiver).receive();
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg);
        verify(messageReceiver).complete(lock);

    }

    @Test
    public void should_dead_letter_the_message_when_unrecoverable_failure() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);

        UUID lock = UUID.randomUUID();
        given(message.getLockToken()).willReturn(lock);

        given(messageReceiver.receive()).willReturn(message);

        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("JsonException"));

        // when
        notificationMessageProcessor.processNextMessage();

        // then
        verify(messageReceiver).receive();

        verify(messageReceiver).deadLetter(
            eq(lock),
            eq("Notification Message processing error"),
            eq("UNRECOVERABLE_FAILURE"));
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_not_dead_letter_the_message_when_recoverable_failure() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);

        given(messageReceiver.receive()).willReturn(message);

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);

        Exception processingFailureCause = new RuntimeException(
            "exception of type treated as recoverable"
        );

        // given an error occurs during message processing
        willThrow(processingFailureCause)
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg);
        // when
        notificationMessageProcessor.processNextMessage();

        // then the message is not finalised (completed/dead-lettered)
        verify(messageReceiver).receive();
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg);
        verifyNoMoreInteractions(messageReceiver);

    }

    @Test
    public void should_dead_letter_the_message_when_recoverable_failure_but_delivery_maxed() throws Exception {
        // given
        given(message.getMessageBody()).willReturn(messageBody);
        given(message.getDeliveryCount()).willReturn(4L);

        UUID lock = UUID.randomUUID();
        given(message.getLockToken()).willReturn(lock);
        given(messageReceiver.receive()).willReturn(message);

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);

        Exception processingFailureCause = new RuntimeException(
            "exception of type treated as recoverable"
        );

        // and an error occurs during message processing
        willThrow(processingFailureCause).given(notificationMessageHandler).handleNotificationMessage(notificationMsg);

        // when
        notificationMessageProcessor.processNextMessage();

        // then the message is dead-lettered
        verify(messageReceiver).deadLetter(
            eq(lock),
            eq("Too many deliveries"),
            eq("Reached limit of message delivery count of 5")
        );
    }

    @Test
    public void should_throw_exception_when_message_receiver_fails() throws Exception {
        ServiceBusException receiverException = new ServiceBusException(true);
        willThrow(receiverException).given(messageReceiver).receive();

        assertThatThrownBy(() -> notificationMessageProcessor.processNextMessage())
            .isSameAs(receiverException);
        verifyNoMoreInteractions(notificationMessageParser);
        verifyNoMoreInteractions(notificationMessageHandler);

    }
}