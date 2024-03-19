package uk.gov.hmcts.reform.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

public class JmsNotificationsMessageSender {

    private static final Logger log = LoggerFactory.getLogger(JmsNotificationsMessageSender.class);

    /**
     * The `send` method sends a `NotificationMsg` object as a JSON string to a JMS queue named "notifications" after
     * setting up the JmsTemplate with a connection factory and a receive timeout of 5 seconds.
     *
     * @param notificationMsg The `send` method you provided is responsible for sending a `NotificationMsg`
     *                        object to a JMS queue named "notifications". The `NotificationMsg` object is
     *                        serialized to JSON format using an `ObjectMapper` with a property naming strategy
     *                        that converts property names to snake case.
     */
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

    /**
     * The function returns a ConnectionFactory configured with ActiveMQ settings for connecting
     * to a local AMQP server with specific credentials and redelivery policies.
     *
     * @return A ConnectionFactory object is being returned.
     */
    public ConnectionFactory getTestFactory() {
        String connection = String.format("amqp://localhost:%1s?amqp.idleTimeout=%2d", "5672", 30000);
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(connection);
        activeMQConnectionFactory.setUserName("admin");
        activeMQConnectionFactory.setPassword("admin");
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(3);
        activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
        activeMQConnectionFactory.setClientID("clientId");
        return new CachingConnectionFactory(activeMQConnectionFactory);
    }
}
