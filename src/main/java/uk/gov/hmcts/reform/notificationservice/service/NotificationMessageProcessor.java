package uk.gov.hmcts.reform.notificationservice.service;

import com.microsoft.azure.servicebus.IMessage;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.exception.UnknownMessageProcessingResultException;

@Service
public class NotificationMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(NotificationMessageProcessor.class);

    private final IMessageReceiver messageReceiver;
    private final NotificationMessageHandler notificationMessageHandler;
    private final NotificationMessageParser notificationMessageParser;
    private final int maxDeliveryCount;

    public NotificationMessageProcessor(
        IMessageReceiver messageReceiver,
        NotificationMessageHandler notificationMessageHandler,
        NotificationMessageParser notificationMessageParser,
        @Value("${queue.notifications.max-delivery-count}") int maxDeliveryCount
    ) {
        this.messageReceiver = messageReceiver;
        this.notificationMessageHandler = notificationMessageHandler;
        this.notificationMessageParser = notificationMessageParser;
        this.maxDeliveryCount = maxDeliveryCount;
    }

    /**
     * Reads and processes next message from the queue.
     *
     * @return false if there was no message to process. Otherwise true.
     */
    public boolean processNextMessage() throws ServiceBusException, InterruptedException {
        log.info("Getting notification message.");
        IMessage message = messageReceiver.receive();
        if (message != null) {
            try {
                // DO NOT CHANGE, used in alert
                log.info("Started processing notification message with ID {}", message.getMessageId());
                var notificationMsg = notificationMessageParser.parse(message.getMessageBody());
                notificationMessageHandler.handleNotificationMessage(notificationMsg, message.getMessageId());
                finaliseProcessedMessage(message, MessageProcessingResult.SUCCESS);
            } catch (InvalidMessageException ex) {
                log.error("Invalid notification message with ID: {} ", message.getMessageId(), ex);
                finaliseProcessedMessage(message, MessageProcessingResult.UNRECOVERABLE_FAILURE);
            } catch (DuplicateMessageIdException ex) {
                handleDuplicateMessageId(message, ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed to process notification message with ID: {} ", message.getMessageId(), ex);
                finaliseProcessedMessage(message, MessageProcessingResult.POTENTIALLY_RECOVERABLE_FAILURE);
            }
        } else {
            log.debug("No notification messages to process by notification processor.");
        }

        return message != null;
    }

    private void handleDuplicateMessageId(IMessage message, String errorMessage)
        throws InterruptedException, ServiceBusException {
        if (message.getDeliveryCount() == 0) {
            deadLetterTheMessage(
                message,
                "Duplicate notification message id",
                errorMessage
            );
        } else {
            log.warn(
                "Notification message already processed for message id: {} Reason: {}",
                message.getMessageId(),
                errorMessage
            );
            messageReceiver.complete(message.getLockToken());
        }
    }

    private void finaliseProcessedMessage(IMessage message, MessageProcessingResult processingResult) {
        try {
            log.info("Finalising Notification Message with ID {} ", message.getMessageId());
            completeProcessedMessage(message, processingResult);
        } catch (InterruptedException ex) {
            log.error(
                "Failed to finalise notification message with ID {}. Processing result: {}",
                message.getMessageId(),
                processingResult,
                ex
            );
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.error(
                "Failed to finalise notification message with ID {}. Processing result: {}",
                message.getMessageId(),
                processingResult,
                ex
            );
        }
    }

    private void completeProcessedMessage(
        IMessage message,
        MessageProcessingResult processingResult
    ) throws InterruptedException, ServiceBusException {

        switch (processingResult) {
            case SUCCESS:
                log.info("Completing Notification Message with ID {} ", message.getMessageId());
                messageReceiver.complete(message.getLockToken());
                log.info("Notification Message with ID {} has been completed successfully.", message.getMessageId());
                break;
            case UNRECOVERABLE_FAILURE:
                deadLetterTheMessage(
                    message,
                    "Notification Message processing error",
                    "UNRECOVERABLE_FAILURE"
                );
                break;
            case POTENTIALLY_RECOVERABLE_FAILURE:
                deadLetterIfMaxDeliveryCountIsReached(message);
                break;
            default:
                throw new UnknownMessageProcessingResultException(
                    "Unknown notification message processing result type: " + processingResult
                );
        }
    }

    private void deadLetterIfMaxDeliveryCountIsReached(IMessage message)
        throws InterruptedException, ServiceBusException {

        int deliveryCount = (int) message.getDeliveryCount() + 1;

        if (deliveryCount < maxDeliveryCount) {
            // do nothing - let the message lock expire
            log.info(
                "Allowing notification message with ID {} to return to queue (delivery attempt {})",
                message.getMessageId(),
                deliveryCount
            );
        } else {
            deadLetterTheMessage(
                message,
                "Too many deliveries",
                "Reached limit of message delivery count of " + deliveryCount
            );
        }
    }

    private void deadLetterTheMessage(
        IMessage message,
        String reason,
        String description
    ) throws InterruptedException, ServiceBusException {
        messageReceiver.deadLetter(
            message.getLockToken(),
            reason,
            description
        );

        log.error(
            "Notification Message with ID {} has been dead-lettered. Reason: '{}'. Description: '{}'",
            message.getMessageId(),
            reason,
            description
        );
    }

    private enum MessageProcessingResult {
        SUCCESS,
        UNRECOVERABLE_FAILURE,
        POTENTIALLY_RECOVERABLE_FAILURE
    }
}
