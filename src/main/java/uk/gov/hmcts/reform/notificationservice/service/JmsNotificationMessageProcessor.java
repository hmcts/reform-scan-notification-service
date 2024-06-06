package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import jakarta.jms.JMSException;
import org.apache.activemq.command.ActiveMQMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.exception.DuplicateMessageIdException;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.exception.UnknownMessageProcessingResultException;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

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
    public void processNextMessage(ActiveMQMessage message, String messageBody) throws JMSException {
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

    private void handleDuplicateMessageId(ActiveMQMessage messageContext, String errorMessage) throws JMSException {
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

    private void finaliseProcessedMessage(
        ActiveMQMessage messageContext,
        MessageProcessingResult processingResult
    ) {
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

    private void completeProcessedMessage(
        ActiveMQMessage messageContext,
        MessageProcessingResult processingResult
    ) throws jakarta.jms.JMSException {
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

    private void deadLetterIfMaxDeliveryCountIsReached(ActiveMQMessage messageContext) throws jakarta.jms.JMSException {
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

    private void deadLetterTheMessage(
        ActiveMQMessage messageContext,
        String reason,
        String description
    ) {
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
