package uk.gov.hmcts.reform.notificationservice.config.jms;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.stereotype.Component;

@Configuration
@EnableJms
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JmsConfiguration {

    @Value("${jms.namespace}")
    private String namespace;

    @Value("${jms.username}")
    private String username;

    @Value("${jms.password}")
    private String password;

    @Value("${jms.receiveTimeout}")
    private Long receiveTimeout;

    @Value("${jms.idleTimeout}")
    private Long idleTimeout;

    @Value("${jms.amqp-connection-string-template}")
    public String amqpConnectionStringTemplate;

    @Primary
    @Bean
    public JmsProperties jmsProperties() {
        return new JmsProperties();
    }

    /**
     * The function creates a JMS ConnectionFactory bean with ActiveMQ configuration and redelivery policy settings.
     *
     * @param clientId The `clientId` parameter in the `notificationsJmsConnectionFactory` method is used to
     *                 set the client ID for the JMS connection. This client ID is typically used to uniquely
     *                 identify a client when connecting to a JMS broker. It helps in distinguishing
     *                 different clients that are connected to the same broker.
     * @return A `CachingConnectionFactory` is being returned, which wraps an `ActiveMQConnectionFactory`
     *      configured with the provided parameters such as connection details, username, password,
     *      redelivery policy, and client ID.
     */
    @Bean
    public ConnectionFactory notificationsJmsConnectionFactory(@Value("${jms.application-name}")
                                                                   final String clientId) {
        String connection = String.format(amqpConnectionStringTemplate, namespace, idleTimeout);
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(connection);
        activeMQConnectionFactory.setUserName(username);
        activeMQConnectionFactory.setPassword(password);
        RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
        redeliveryPolicy.setMaximumRedeliveries(3);
        activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);
        activeMQConnectionFactory.setClientID(clientId);
        return new CachingConnectionFactory(activeMQConnectionFactory);
    }

    /**
     * This function creates and configures a JmsTemplate bean with a specified connection factory
     * and receive timeout of 5 seconds.
     *
     * @param connectionFactory The `connectionFactory` parameter in the `jmsTemplate` method is an instance
     *                          of `javax.jms.ConnectionFactory`. It is used to create connections to the JMS
     *                          provider, which is necessary for sending and receiving messages to and from a
     *                          JMS destination.
     * @return An instance of JmsTemplate with the receive timeout set to 5 seconds is being returned.
     */
    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setConnectionFactory(connectionFactory);
        jmsTemplate.setReceiveTimeout(5000); // Set the receive timeout to 5 seconds
        return jmsTemplate;
    }

    /**
     * This function configures a JMS listener container factory for a notifications event queue in Java.
     *
     * @param notificationsJmsConnectionFactory The `notificationsJmsConnectionFactory` parameter in the
     *                                          method `notificationsEventQueueContainerFactory` is of type
     *                                          `ConnectionFactory`. This parameter is used to provide the
     *                                          connection factory for the JMS listener container factory to
     *                                          establish a connection to the JMS provider (message broker)
     *                                          for sending and receiving messages.
     * @param jmsProperties The `jmsProperties` parameter in your method `notificationsEventQueueContainerFactory`
     *                      is of type `JmsProperties`. This parameter likely contains configuration properties
     *                      related to the JMS (Java Message Service) setup in your application.
     * @return The method `notificationsEventQueueContainerFactory` is returning an instance of
     *      `DefaultJmsListenerContainerFactory` configured with various settings such as session acknowledge
     *      mode, connection factory, receive timeout, session transacted, message converter, and whether it is
     *      using a publish-subscribe domain based on the value of `jmsProperties.isPubSubDomain()`.
     */
    @Bean
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> notificationsEventQueueContainerFactory(
        ConnectionFactory notificationsJmsConnectionFactory,
        JmsProperties jmsProperties) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setSessionAcknowledgeMode(2);
        factory.setConnectionFactory(notificationsJmsConnectionFactory);
        factory.setReceiveTimeout(receiveTimeout);
        factory.setSessionTransacted(Boolean.TRUE);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        factory.setMessageConverter(new CustomMessageConverter());
        factory.setPubSubDomain(jmsProperties.isPubSubDomain());
        return factory;
    }

    /**
     * The `CustomMessageConverter` class is a custom implementation of the `MessageConverter` interface for converting
     * objects to JMS messages and vice versa.
     */
    @Component
    public static class CustomMessageConverter implements MessageConverter {

        /**
         * The function converts an object to a JMS Message by creating a TextMessage with the object's string
         * representation.
         *
         * @param object The `object` parameter in the `toMessage` method represents the object that you want
         *               to convert into a JMS message.
         * @param session The `session` parameter in the `toMessage` method is an object representing
         *                a connection to the JMS provider. It is typically used to create JMS messages,
         *                manage transactions, and interact with the JMS provider.
         * @return A TextMessage is being returned.
         */
        @Override
        public Message toMessage(Object object, Session session) throws JMSException, MessageConversionException {
            return session.createTextMessage(object.toString());
        }

        /**
         * The function simply returns the input message as an Object.
         *
         * @param message The `fromMessage` method you provided is a simple implementation that returns the
         *               `Message` object as is. This method is used to convert a message received from a messaging
         *                system into an object that can be processed by your application.
         * @return The method is returning the `Message` object itself.
         */
        @Override
        public Object fromMessage(Message message) throws MessageConversionException {
            return message;
        }
    }
}
