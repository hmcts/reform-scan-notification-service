package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessageContext;
import com.azure.messaging.servicebus.models.DeadLetterOptions;
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

    private final NotificationMessageHandler notificationMessageHandler;
    private final NotificationMessageParser notificationMessageParser;
    private final int maxDeliveryCount;

    public NotificationMessageProcessor(
        NotificationMessageHandler notificationMessageHandler,
        NotificationMessageParser notificationMessageParser,
        @Value("${queue.notifications.max-delivery-count}") int maxDeliveryCount
    ) {
        this.notificationMessageHandler = notificationMessageHandler;
        this.notificationMessageParser = notificationMessageParser;
        this.maxDeliveryCount = maxDeliveryCount;
    }


    /**
     * Reads and processes next message from the queue.
     * return false if there was no message to process. Otherwise true.
     */
    public void processNextMessage(ServiceBusReceivedMessageContext messageContext) {
        ServiceBusReceivedMessage message = messageContext.getMessage();
        if (message != null) {
            try {
                // DO NOT CHANGE, used in alert
                log.info("Started processing notification message with ID {}", message.getMessageId());
                log.info("Start processing notification message, ID {}, locked until {}, expires: {}",
                         message.getMessageId(),
                         message.getLockedUntil(),
                         message.getExpiresAt()
                );
                var notificationMsg = notificationMessageParser.parse(message.getBody());
                notificationMessageHandler.handleNotificationMessage(notificationMsg, message.getMessageId());
                finaliseProcessedMessage(messageContext, MessageProcessingResult.SUCCESS);
            } catch (InvalidMessageException ex) {
                log.error("Invalid notification message with ID: {} ", message.getMessageId(), ex);
                finaliseProcessedMessage(messageContext, MessageProcessingResult.UNRECOVERABLE_FAILURE);
            } catch (DuplicateMessageIdException ex) {
                handleDuplicateMessageId(messageContext, ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed to process notification message with ID: {} ", message.getMessageId(), ex);
                finaliseProcessedMessage(messageContext, MessageProcessingResult.POTENTIALLY_RECOVERABLE_FAILURE);
            }
        } else {
            log.error("Triggered notification queue process but there is no message !!!");
        }
    }

    private void handleDuplicateMessageId(ServiceBusReceivedMessageContext messageContext, String errorMessage) {
        var message = messageContext.getMessage();
        if (message.getDeliveryCount() == 0) {
            deadLetterTheMessage(
                messageContext,
                "Duplicate notification message id",
                errorMessage
            );
        } else {
            log.warn(
                "Notification message already processed for message id: {} Reason: {}",
                message.getMessageId(),
                errorMessage
            );
            messageContext.complete();
        }
    }

    private void finaliseProcessedMessage(
        ServiceBusReceivedMessageContext messageContext,
        MessageProcessingResult processingResult
    ) {
        var message = messageContext.getMessage();
        try {
            log.info("Finalising Notification Message with ID {} ", message.getMessageId());
            completeProcessedMessage(messageContext, processingResult);
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
        ServiceBusReceivedMessageContext messageContext,
        MessageProcessingResult processingResult
    ) {
        var message = messageContext.getMessage();
        switch (processingResult) {
            case SUCCESS:
                log.info("Completing Notification Message with ID {} ", message.getMessageId());
                messageContext.complete();
                log.info("Notification Message with ID {} has been completed successfully.", message.getMessageId());
                break;
            case UNRECOVERABLE_FAILURE:
                deadLetterTheMessage(
                    messageContext,
                    "Notification Message processing error",
                    "UNRECOVERABLE_FAILURE"
                );
                break;
            case POTENTIALLY_RECOVERABLE_FAILURE:
                deadLetterIfMaxDeliveryCountIsReached(messageContext);
                break;
            default:
                throw new UnknownMessageProcessingResultException(
                    "Unknown notification message processing result type: " + processingResult
                );
        }
    }

    private void deadLetterIfMaxDeliveryCountIsReached(ServiceBusReceivedMessageContext messageContext) {
        var message = messageContext.getMessage();
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
                messageContext,
                "Too many deliveries",
                "Reached limit of message delivery count of " + deliveryCount
            );
        }
    }

    private void deadLetterTheMessage(
        ServiceBusReceivedMessageContext messageContext,
        String reason,
        String description
    ) {
        messageContext
            .deadLetter(new DeadLetterOptions().setDeadLetterReason(reason).setDeadLetterErrorDescription(description));

        log.error(
            "Notification Message with ID {} has been dead-lettered. Reason: '{}'. Description: '{}'",
            messageContext.getMessage().getMessageId(),
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
