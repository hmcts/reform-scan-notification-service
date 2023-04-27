package uk.gov.hmcts.reform.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultRedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import javax.jms.ConnectionFactory;

public class JmsNotificationsMessageSender {

    private static final Logger log = LoggerFactory.getLogger(JmsNotificationsMessageSender.class);

    public void send(NotificationMsg notificationMsg) {
        try {
            JmsTemplate jmsTemplate = new JmsTemplate();
            jmsTemplate.setConnectionFactory(getTestFactory());
            jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds

            // to make sure json props have _'s between values
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

            jmsTemplate.convertAndSend("notifications",
                                       objectMapper.writeValueAsString(notificationMsg));

            log.info(
                "Sent message to notifications queue. Content: {}",
                notificationMsg
            );
        } catch (Exception ex) {
            throw new RuntimeException(
                "An error occurred when trying to publish message to notifications queue.",
                ex
            );
        }
    }

    public ConnectionFactory getTestFactory() {
        String connection = String.format("amqp://localhost:%1s?amqp.idleTimeout=%2d", "5672", 30000);
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connection);
        jmsConnectionFactory.setUsername("admin");
        jmsConnectionFactory.setPassword("admin");
        JmsDefaultRedeliveryPolicy jmsDefaultRedeliveryPolicy = new JmsDefaultRedeliveryPolicy();
        jmsDefaultRedeliveryPolicy.setMaxRedeliveries(3);
        jmsConnectionFactory.setRedeliveryPolicy(jmsDefaultRedeliveryPolicy);
        jmsConnectionFactory.setClientID("clientId");
        return new CachingConnectionFactory(jmsConnectionFactory);
    }
}
