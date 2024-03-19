package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.exception.UnknownMessageProcessingResultException;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The `JmsNotificationMessageProcessor` class in Java processes notification messages from a queue, handling various
 * scenarios such as message parsing, duplicate handling, and finalizing message processing.
 */
@Service
public class JmsNotificationMessageProcessor {

    private static final Logger log = LoggerFactory.getLogger(JmsNotificationMessageProcessor.class);

    private final NotificationMessageHandler notificationMessageHandler;
    private final NotificationMessageParser notificationMessageParser;
    private final int maxDeliveryCount;

    public JmsNotificationMessageProcessor(
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
    public void processNextMessage(Message message, String messageBody) throws JMSException {
        if (message != null && !messageBody.isEmpty()) {
            try {
                // DO NOT CHANGE, used in alert
                log.info("Started processing notification message with ID {}", message.getJMSMessageID());
                NotificationMsg notificationMsg = notificationMessageParser.parse(BinaryData.fromString(messageBody));
                notificationMessageHandler.handleNotificationMessage(notificationMsg, message.getJMSMessageID());
                finaliseProcessedMessage(message, MessageProcessingResult.SUCCESS);
            } catch (InvalidMessageException ex) {
                log.error("Invalid notification message with ID: {} ", message.getJMSMessageID(), ex);
                finaliseProcessedMessage(message, MessageProcessingResult.UNRECOVERABLE_FAILURE);
            } catch (DuplicateMessageIdException ex) {
                handleDuplicateMessageId(message, ex.getMessage());
            } catch (Exception ex) {
                log.error("Failed to process notification message with ID: {} ", message.getJMSMessageID(), ex);
                finaliseProcessedMessage(message, MessageProcessingResult.POTENTIALLY_RECOVERABLE_FAILURE);
            }
        } else {
            log.error("Triggered notification queue process but there is no message !!!");
        }
    }

    /**
     * The function `handleDuplicateMessageId` checks if a message has already been processed and logs a warning if so.
     *
     * @param messageContext The `messageContext` parameter in the `handleDuplicateMessageId` method is
     *                       of type `Message` and represents the message being processed. It contains
     *                       information and properties related to the message, such as the message ID,
     *                       delivery count, and error message.
     * @param errorMessage The `errorMessage` parameter in the `handleDuplicateMessageId` method is a string
     *                     that represents the reason for the duplicate message handling. It is
     *                     used in the log message to provide information about why the notification
     *                     message was already processed for a specific message ID.
     */
    private void handleDuplicateMessageId(Message messageContext, String errorMessage) throws JMSException {
        if (messageContext.getStringProperty("JMSXDeliveryCount").equals("0")) {
            log.error("Message dead-lettered...if this was ASB");
        } else {
            log.warn(
                "Notification message already processed for message id: {} Reason: {}",
                messageContext.getJMSMessageID(),
                errorMessage
            );
            messageContext.acknowledge();
        }
    }

    /**
     * The `finaliseProcessedMessage` method logs information about finalizing a notification message and catches any
     * exceptions that occur during the process.
     *
     * @param messageContext The `messageContext` parameter in the `finaliseProcessedMessage` method represents
     *                       the message being processed. It contains information about the message, such
     *                       as its ID (`JMSMessageID`) and other relevant details needed for processing
     *                       and finalizing the message.
     * @param processingResult The `processingResult` parameter is of type `MessageProcessingResult` and is
     *                         used to store the result of processing a message. It likely contains
     *                         information such as whether the processing was successful, any errors
     *                         encountered, or any relevant data related to the processing of the message.
     */
    private void finaliseProcessedMessage(
        Message messageContext,
        MessageProcessingResult processingResult
    ) throws JMSException {
        try {
            log.info("Finalising Notification Message with ID {} ", messageContext.getJMSMessageID());
            completeProcessedMessage(messageContext, processingResult);
        } catch (Exception ex) {
            log.error(
                "Failed to finalise notification message with ID {}. Processing result: {}",
                messageContext.getJMSMessageID(),
                processingResult,
                ex
            );
        }
    }

    /**
     * The `completeProcessedMessage` function processes a message based on the result of message processing and handles
     * success, unrecoverable failure, potentially recoverable failure, and unknown result cases.
     *
     * @param messageContext The `messageContext` parameter in the `completeProcessedMessage` method
     *                       represents the message that was processed and needs to be completed.
     *                       It contains information about the message, such as its ID (`JMSMessageID`) and
     *                       allows actions like acknowledging the message.
     * @param processingResult The `processingResult` parameter in the `completeProcessedMessage` method
     *                         represents the result of processing a message.
     */
    private void completeProcessedMessage(
        Message messageContext,
        MessageProcessingResult processingResult
    ) throws JMSException {
        switch (processingResult) {
            case SUCCESS -> {
                log.info("Completing Notification Message with ID {} ", messageContext.getJMSMessageID());
                messageContext.acknowledge();
                log.info(
                    "Notification Message with ID {} has been completed successfully.",
                    messageContext.getJMSMessageID()
                );
            }
            case UNRECOVERABLE_FAILURE -> deadLetterTheMessage(
                messageContext,
                "Notification Message processing error",
                "UNRECOVERABLE_FAILURE"
            );
            case POTENTIALLY_RECOVERABLE_FAILURE -> deadLetterIfMaxDeliveryCountIsReached(messageContext);
            default -> throw new UnknownMessageProcessingResultException(
                "Unknown notification message processing result type: " + processingResult
            );
        }
    }

    /**
     * The function `deadLetterIfMaxDeliveryCountIsReached` checks if the maximum delivery count is reached for a
     * message and either allows it to return to the queue or moves it to a dead letter queue accordingly.
     *
     * @param messageContext The `messageContext` parameter in the `deadLetterIfMaxDeliveryCountIsReached`
     *                       method represents the message that is being processed. It contains information
     *                       about the message, such as its delivery count, message ID, and properties.
     *                       The method checks the delivery count of the message against a maximum delivery count
     */
    private void deadLetterIfMaxDeliveryCountIsReached(Message messageContext) throws JMSException {
        int deliveryCount = (Integer.parseInt(messageContext.getStringProperty("JMSXDeliveryCount")) + 1);

        if (deliveryCount < maxDeliveryCount) {
            // do nothing - let the message lock expire
            log.info(
                "Allowing notification message with ID {} to return to queue (delivery attempt {})",
                messageContext.getJMSMessageID(),
                deliveryCount
            );
            throw new JMSException("Max attempts not reached, retrying by sending back to the queue");
        } else {
            deadLetterTheMessage(
                messageContext,
                "Too many deliveries",
                "Reached limit of message delivery count of " + deliveryCount
            );
        }
    }

    /**
     * The `deadLetterTheMessage` function logs an error message indicating that a notification message has been
     * dead-lettered with the provided reason and description.
     *
     * @param messageContext The `messageContext` parameter in the `deadLetterTheMessage`
     *                       method is of type `Message` and represents the message context or the
     *                       message that needs to be dead-lettered.
     * @param reason The `reason` parameter in the `deadLetterTheMessage` method is used to specify the
     *               reason why the message is being dead-lettered. It provides a brief explanation or
     *               categorization of why the message could not be processed successfully and had to be
     *               moved to the dead-letter queue.
     * @param description The `description` parameter in the `deadLetterTheMessage` method is a string
     *                    that provides additional information or details about why the message was
     *                    dead-lettered. It is used to give more context or explanation about the
     *                    reason for dead-lettering the message.
     */
    private void deadLetterTheMessage(
        Message messageContext,
        String reason,
        String description
    ) throws JMSException {
        log.error(
            "Notification Message with ID {} has been dead-lettered (if this was ASB). Reason: '{}'. Description: '{}'",
            messageContext.getJMSMessageID(),
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
