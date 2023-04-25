package uk.gov.hmcts.reform.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultRedeliveryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.jms.ConnectionFactory;

@Service
public class JmsNotificationsMessageSender {

    private static final Logger log = LoggerFactory.getLogger(JmsNotificationsMessageSender.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(QueueMessageDetails cmd) {
        try {
            final String messageContent = objectMapper.writeValueAsString(cmd);

            JmsTemplate jmsTemplate = new JmsTemplate();
            jmsTemplate.setConnectionFactory(getTestFactory());
            jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds

            jmsTemplate.convertAndSend("notifications", messageContent);

            log.info(
                "Sent message to notifications queue. Content: {}",
                messageContent
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
