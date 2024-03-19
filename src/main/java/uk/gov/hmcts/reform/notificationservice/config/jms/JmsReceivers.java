package uk.gov.hmcts.reform.notificationservice.config.jms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import uk.gov.hmcts.reform.notificationservice.service.JmsNotificationMessageProcessor;

import javax.jms.JMSException;
import javax.jms.Message;

/**
 * The `JmsReceivers` class in Java listens for messages on a JMS destination, processes the message content, and logs
 * relevant information.
 */
@Configuration()
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsReceivers {

    private static final Logger log = LoggerFactory.getLogger(JmsReceivers.class);

    private final JmsNotificationMessageProcessor jmsNotificationMessageProcessor;

    public JmsReceivers(
        JmsNotificationMessageProcessor jmsNotificationMessageProcessor
    ) {
        this.jmsNotificationMessageProcessor = jmsNotificationMessageProcessor;
    }

    /**
     * This Java function listens for messages on a JMS destination, processes the message content, and logs relevant
     * information.
     *
     * @param message The `message` parameter in the `receiveMessage` method represents the JMS message
     *                that is received from the "notifications" destination. It is of type `Message`
     *                which is a generic JMS message type.
     */
    @JmsListener(destination = "notifications", containerFactory = "notificationsEventQueueContainerFactory")
    public void receiveMessage(Message message) throws JMSException {
        String messageBody = ((javax.jms.TextMessage) message).getText();
        log.info("Received Message {} on Service Bus. Delivery count is: {}",
                 messageBody, message.getStringProperty("JMSXDeliveryCount"));
        jmsNotificationMessageProcessor.processNextMessage(message, messageBody);
        log.info("Message finished/completed");
    }
}
