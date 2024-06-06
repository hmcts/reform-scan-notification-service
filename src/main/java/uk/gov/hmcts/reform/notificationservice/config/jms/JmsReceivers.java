package uk.gov.hmcts.reform.notificationservice.config.jms;

import jakarta.jms.JMSException;
import org.apache.activemq.command.ActiveMQMessage;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.JmsListener;
import uk.gov.hmcts.reform.notificationservice.service.JmsNotificationMessageProcessor;

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

    @JmsListener(destination = "notifications", containerFactory = "notificationsEventQueueContainerFactory")
    public void receiveMessage(ActiveMQMessage message) throws JMSException {
        String messageBody = ((ActiveMQTextMessage) message).getText();
        log.info("Received Message {} on Service Bus. Delivery count is: {}",
                 messageBody, message.getStringProperty("JMSXDeliveryCount"));
        log.info(messageBody);
        jmsNotificationMessageProcessor.processNextMessage(message, messageBody);
        log.info("Message finished/completed");
    }
}
