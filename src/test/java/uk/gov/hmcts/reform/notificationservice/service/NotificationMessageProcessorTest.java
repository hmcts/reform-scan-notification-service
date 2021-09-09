package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusErrorSource;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ServiceBusReceivedMessageContext messageContext;
    @Mock
    private NotificationMessageHandler notificationMessageHandler;
    @Mock
    private NotificationMessageParser notificationMessageParser;
    @Mock
    private ServiceBusReceivedMessage message;
    @Mock
    private NotificationMsg notificationMsg;
    @Mock
    private BinaryData messageBody;


    @BeforeEach
    void before() {
        notificationMessageProcessor = new NotificationMessageProcessor(
            notificationMessageHandler,
            notificationMessageParser,
            5
        );
    }


    @Test
    void should_not_throw_exception_when_queue_message_is_invalid() throws Exception {
        // given
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);
        given(messageContext.getMessage()).willReturn(message);

        var messageBody = mock(BinaryData.class);
        given(message.getBody()).willReturn(messageBody);

        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("Invalid Message"));

        //when
        notificationMessageProcessor.processNextMessage(messageContext);
        // then
        verify(notificationMessageParser).parse(messageBody);
        verifyNoMoreInteractions(notificationMessageHandler);

    }

    @Test
    void should_not_throw_exception_when_notification_handler_fails() {
        // given
        String messageId = mockQueueMessageAndParse();

        willThrow(new RuntimeException("Handle exception"))
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);

        notificationMessageProcessor.processNextMessage(messageContext);
        //when

        // then
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
    }

    @Test
    void should_complete_the_message_when_processing_is_successful() {
        // given
        String messageId = mockQueueMessageAndParse();

        NotificationMsg notificationMsg = mock(NotificationMsg.class);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);

        doNothing().when(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);

        // when
        notificationMessageProcessor.processNextMessage(messageContext);

        // then
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
        verify(messageContext).complete();
    }

    @Test
    void should_dead_letter_the_message_when_unrecoverable_failure() {
        // given
        mockQueueMessageAndParse();

        given(notificationMessageParser.parse(messageBody)).willThrow(new InvalidMessageException("JsonException"));

        // when
        notificationMessageProcessor.processNextMessage(messageContext);

        // then
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageContext).deadLetter(
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Notification Message processing error");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("UNRECOVERABLE_FAILURE");
    }

    @Test
    void should_not_dead_letter_the_message_when_recoverable_failure() throws Exception {
        // given
        var messageId = mockQueueMessageAndParse();

        Exception processingFailureCause = new RuntimeException(
            "exception of type treated as recoverable"
        );

        // given an error occurs during message processing
        willThrow(processingFailureCause)
            .given(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
        // when
        notificationMessageProcessor.processNextMessage(messageContext);

        // then the message is not finalised (completed/dead-lettered)
        verify(notificationMessageParser).parse(messageBody);
        verify(notificationMessageHandler).handleNotificationMessage(notificationMsg, messageId);
    }

    @Test
    void should_dead_letter_the_message_when_recoverable_failure_but_delivery_maxed() {
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
        notificationMessageProcessor.processNextMessage(messageContext);

        // then the message is dead-lettered
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);
        verify(messageContext).deadLetter(
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Too many deliveries");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("Reached limit of message delivery count of 5");
    }

    @Test
    void should_throw_exception_when_message_receiver_fails() {
        ServiceBusException receiverException = new ServiceBusException(
            new RuntimeException("Test service bus exception "),
            ServiceBusErrorSource.ABANDON
        );
        willThrow(receiverException).given(messageContext).getMessage();

        assertThatThrownBy(() -> notificationMessageProcessor.processNextMessage(messageContext))
            .isSameAs(receiverException);
        verifyNoMoreInteractions(notificationMessageParser);
        verifyNoMoreInteractions(notificationMessageHandler);
    }

    @Test
    void should_dead_letter_the_message_when_duplicate_messageId_received_for_already_processed_message() {
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
        notificationMessageProcessor.processNextMessage(messageContext);

        // then the message should be dead-lettered because it is the first delivery of the message
        ArgumentCaptor<DeadLetterOptions> deadLetterOptionsArgumentCaptor
            = ArgumentCaptor.forClass(DeadLetterOptions.class);

        verify(messageContext).deadLetter(
            deadLetterOptionsArgumentCaptor.capture()
        );

        var deadLetterOptions = deadLetterOptionsArgumentCaptor.getValue();
        assertThat(deadLetterOptions.getDeadLetterReason())
            .isEqualTo("Duplicate notification message id");
        assertThat(deadLetterOptions.getDeadLetterErrorDescription())
            .isEqualTo("Duplicate message Id received. messageId: " + messageId);
    }

    @Test
    void should_complete_the_message_when_duplicate_messageId_received_for_already_delivered_message() {
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
        notificationMessageProcessor.processNextMessage(messageContext);

        // then the message should be completed because the message was delivered already
        verify(messageContext).complete();
    }

    private void mockMessageDetails(String messageId) {
        given(message.getMessageId()).willReturn(messageId);
        given(message.getLockedUntil()).willReturn(OffsetDateTime.now());
        given(message.getExpiresAt()).willReturn(OffsetDateTime.now());

    }

    private String mockQueueMessageAndParse() {
        String messageId = UUID.randomUUID().toString();
        mockMessageDetails(messageId);
        given(message.getBody()).willReturn(messageBody);
        given(notificationMessageParser.parse(messageBody)).willReturn(notificationMsg);
        given(messageContext.getMessage()).willReturn(message);
        return messageId;
    }
}
