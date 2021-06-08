package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
    private ServiceBusReceiverClient messageReceiver;
    @Mock
    private NotificationMessageHandler notificationMessageHandler;
    @Mock
    private NotificationMessageParser notificationMessageParser;
    @Mock
    private IterableStream<ServiceBusReceivedMessage> iterableStream;
    @Mock
    private ServiceBusReceivedMessage message;
    @Mock
    private NotificationMsg notificationMsg;
    @Mock
    private BinaryData messageBody;

    @BeforeEach
    public void before() {
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
        var messageId = mockQueueMessageAndParse();

        // when
        boolean processedMessage = notificationMessageProcessor.processNextMessage();

        // then
        assertThat(processedMessage).isTrue();
        verify(messageReceiver).receiveMessages(1);
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);

    }

    @Test
    public void should_return_false_when_there_is_no_message_to_process() throws Exception {
        // given
        Stream<ServiceBusReceivedMessage>
            messageStream = Stream.empty();
        given(iterableStream.stream()).willReturn(messageStream);
        given(messageReceiver.receiveMessages(1)).willReturn(iterableStream);

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
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);

        Stream<ServiceBusReceivedMessage>
            messageStream = List.of(message).stream();
        given(iterableStream.stream()).willReturn(messageStream);
        given(messageReceiver.receiveMessages(1)).willReturn(iterableStream);
        var messageBody = mock(BinaryData.class);
        given(message.getBody()).willReturn(messageBody);

        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("Invalid Message"));

        //when
        assertThat(notificationMessageProcessor.processNextMessage()).isTrue();

        // then
        verify(messageReceiver).receiveMessages(1);
        verify(notificationMessageParser).parse(messageBody);
        verifyNoMoreInteractions(notificationMessageHandler);
    }

    @Test
    public void should_not_throw_exception_when_notification_handler_fails() throws Exception {
        // given
        String messageId = mockQueueMessageAndParse();

        willThrow(new RuntimeException("Handle exception"))
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);

        //when
        assertThatCode(() -> notificationMessageProcessor.processNextMessage()).doesNotThrowAnyException();

        // then
        verify(messageReceiver).receiveMessages(1);
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
    }

    @Test
    public void should_complete_the_message_when_processing_is_successful() throws Exception {
        // given
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);

        Stream<ServiceBusReceivedMessage>
            messageStream = List.of(message).stream();
        given(iterableStream.stream()).willReturn(messageStream);
        given(messageReceiver.receiveMessages(1)).willReturn(iterableStream);
        var messageBody = mock(BinaryData.class);
        given(message.getBody()).willReturn(messageBody);


        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);

        doNothing().when(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);

        // when
        notificationMessageProcessor.processNextMessage();

        // then
        verify(messageReceiver).receiveMessages(1);
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
        verify(messageReceiver).complete(message);

    }

    @Test
    public void should_dead_letter_the_message_when_unrecoverable_failure() throws Exception {
        // given
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);

        Stream<ServiceBusReceivedMessage>
            messageStream = List.of(message).stream();
        given(iterableStream.stream()).willReturn(messageStream);
        given(messageReceiver.receiveMessages(1)).willReturn(iterableStream);
        var messageBody = mock(BinaryData.class);
        given(message.getBody()).willReturn(messageBody);

        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("JsonException"));

        // when
        notificationMessageProcessor.processNextMessage();

        // then
        verify(messageReceiver).receiveMessages(1);
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageReceiver).deadLetter(
            eq(message),
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Notification Message processing error");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("UNRECOVERABLE_FAILURE");
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_not_dead_letter_the_message_when_recoverable_failure() throws Exception {
        // given
        var messageId = mockQueueMessageAndParse();

        Exception processingFailureCause = new RuntimeException(
            "exception of type treated as recoverable"
        );

        // given an error occurs during message processing
        willThrow(processingFailureCause)
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
        // when
        notificationMessageProcessor.processNextMessage();

        // then the message is not finalised (completed/dead-lettered)
        verify(messageReceiver).receiveMessages(1);
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
        verifyNoMoreInteractions(messageReceiver);

    }

    @Test
    public void should_dead_letter_the_message_when_recoverable_failure_but_delivery_maxed() throws Exception {
        // given
        var messageId = mockQueueMessageAndParse();
        ;
        given(message.getDeliveryCount()).willReturn(4L);

        Exception processingFailureCause = new RuntimeException(
            "exception of type treated as recoverable"
        );

        // and an error occurs during message processing
        willThrow(processingFailureCause).given(notificationMessageHandler)
            .handleNotificationMessage(notificationMsg, messageId);

        // when
        notificationMessageProcessor.processNextMessage();

        // then the message is dead-lettered
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(messageReceiver).receiveMessages(1);
        verify(messageReceiver).deadLetter(
            eq(message),
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Too many deliveries");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("Reached limit of message delivery count of 5");
        verifyNoMoreInteractions(messageReceiver);
    }

    @Test
    public void should_throw_exception_when_message_receiver_fails() throws Exception {
        ServiceBusException receiverException = new ServiceBusException(
            new RuntimeException("Test service bus exception "),
            ServiceBusErrorSource.ABANDON
        );
        willThrow(receiverException).given(messageReceiver).receiveMessages(1);

        assertThatThrownBy(() -> notificationMessageProcessor.processNextMessage())
            .isSameAs(receiverException);
        verifyNoMoreInteractions(notificationMessageParser);
        verifyNoMoreInteractions(notificationMessageHandler);
    }

    @Test
    public void should_dead_letter_the_message_when_duplicate_messageId_received_for_already_processed_message()
        throws Exception {
        // given
        var messageId = mockQueueMessageAndParse();
        ;

        Exception messageIdException = new DuplicateMessageIdException(
            "Duplicate message Id received. messageId: " + messageId
        );

        // throws exception for duplicate message id
        willThrow(messageIdException).given(notificationMessageHandler)
            .handleNotificationMessage(notificationMsg, messageId);

        // when
        notificationMessageProcessor.processNextMessage();

        // then the message should be dead-lettered because it is the first delivery of the message
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageReceiver).deadLetter(
            eq(message),
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Duplicate notification message id");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("Duplicate message Id received. messageId: " + messageId);
    }

    @Test
    public void should_complete_the_message_when_duplicate_messageId_received_for_already_delivered_message()
        throws Exception {
        // given
        var messageId = mockQueueMessageAndParse();
        ;
        given(message.getDeliveryCount()).willReturn(1L); // not the first delivery of the message


        Exception messageIdException = new DuplicateMessageIdException(
            "Duplicate message Id received"
        );

        // throws exception for duplicate message id when processing the message
        willThrow(messageIdException).given(notificationMessageHandler)
            .handleNotificationMessage(notificationMsg, messageId);

        // when
        notificationMessageProcessor.processNextMessage();

        // then the message should be completed because the message was delivered already
        verify(messageReceiver).complete(message);
    }


    private void mockMessageDetails(String messageId) {
        given(message.getMessageId()).willReturn(messageId);
        given(message.getLockedUntil()).willReturn(OffsetDateTime.now());
        given(message.getExpiresAt()).willReturn(OffsetDateTime.now());

    }

    private String mockQueueMessageAndParse() {
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);

        Stream<ServiceBusReceivedMessage>
            messageStream = List.of(message).stream();
        given(iterableStream.stream()).willReturn(messageStream);
        given(messageReceiver.receiveMessages(1)).willReturn(iterableStream);
        given(message.getBody()).willReturn(messageBody);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);
        return messageId;
    }
}
