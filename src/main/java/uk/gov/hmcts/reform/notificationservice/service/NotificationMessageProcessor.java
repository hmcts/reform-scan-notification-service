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

/**
 * The `NotificationMessageProcessor` class in Java processes notification messages from a
 * service bus, handling exceptions and logging relevant information.
 */
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
     * The `processNextMessage` function processes a notification message from a service bus, handling
     * different exceptions and logging relevant information.
     *
     * @param messageContext The `messageContext` parameter in the `processNextMessage` method is of
     *                       type `ServiceBusReceivedMessageContext`. It contains information about the received
     *                       message, such as the message itself and metadata associated with it. This context
     *                       is used to process the message and handle any exceptions that may occur during
     *                       processing.
     */
    public void processNextMessage(ServiceBusReceivedMessageContext messageContext) {
        ServiceBusReceivedMessage message = messageContext.getMessage();
        if (message != null) {
            try {
                // DO NOT CHANGE, used in alert
                log.info("Started processing notification message with ID {}", message.getMessageId());
                log.info(
                    "Start processing notification message, ID {}, locked until {}, expires: {}",
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

    /**
     * The function `handleDuplicateMessageId` checks if a message with a duplicate ID has been processed
     * before and takes appropriate action.
     *
     * @param messageContext The `messageContext` parameter in the `handleDuplicateMessageId` method is of
     *                       type `ServiceBusReceivedMessageContext`. It contains information about the
     *                       received message, such as the message itself and its delivery count.
     * @param errorMessage The `errorMessage` parameter in the `handleDuplicateMessageId` method is a string
     *                     that provides information about the error or reason for handling the duplicate
     *                     message ID. It is used for logging purposes and to provide context about why the
     *                     duplicate message ID is being handled in a specific way.
     */
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

    /**
     * The `finaliseProcessedMessage` method logs information about finalizing a notification message and catches any
     * exceptions that occur during the process.
     *
     * @param messageContext The `messageContext` parameter in the `finaliseProcessedMessage` method is of
     *                       type `ServiceBusReceivedMessageContext` and contains information about the
     *                       received message, such as the message itself and its properties.
     * @param processingResult The `processingResult` parameter in the `finaliseProcessedMessage` method
     *                         represents the result of processing a message. It is used to determine the
     *                         outcome of processing the message, such as whether it was successfully processed
     *                         or if an error occurred during processing.
     */
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

    /**
     * The `completeProcessedMessage` function processes a received message based on the processing result,
     * completing it if successful, dead-lettering it for unrecoverable failures, handling potentially
     * recoverable failures, and throwing an exception for unknown processing results.
     *
     * @param messageContext The `messageContext` parameter in the `completeProcessedMessage` method is of
     *                       type `ServiceBusReceivedMessageContext` and contains information about the received
     *                       message and its processing context. It allows you to access the message
     *                       itself, complete or dead-letter the message, and perform other operations related
     *                       to message processing.
     * @param processingResult The `processingResult` parameter in the `completeProcessedMessage` method
     *                         represents the result of processing a received message.
     */
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

    /**
     * The function `deadLetterIfMaxDeliveryCountIsReached` checks if the delivery count of a message exceeds a maximum
     * limit and dead letters the message if the limit is reached.
     *
     * @param messageContext The `messageContext` parameter in the `deadLetterIfMaxDeliveryCountIsReached` method is
     *                       of type `ServiceBusReceivedMessageContext`. It contains information about the received
     *                       message, such as the message itself and its delivery count.
     */
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

    /**
     * The `deadLetterTheMessage` function dead-letters a message in a Service Bus queue with a specified reason and
     * description, and logs the action.
     *
     * @param messageContext The `ServiceBusReceivedMessageContext` parameter represents the context of
     *                       the received message from a service bus. It contains information about the message
     *                       such as its content, properties, and metadata. In the `deadLetterTheMessage`
     *                       method, this context is used to dead-letter the message with the specified reason.
     * @param reason         The `reason` parameter in the `deadLetterTheMessage` method is used to specify the
     *                       reason for dead-lettering the message. It provides a brief explanation or
     *                       categorization of why the message was moved to the dead-letter queue.
     * @param description    The `description` parameter in the `deadLetterTheMessage` method is a string that
     *                       provides a description or explanation for why the message was dead-lettered. It
     *                       helps in providing more context or details about the reason for dead-lettering
     *                       the message.
     */
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
