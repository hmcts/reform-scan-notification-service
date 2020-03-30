package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.read-connection-string")
    public IMessageReceiver notificationsMessageReceiver(
        @Value("${queue.notifications.read-connection-string}") String connectionString)
        throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageReceiverFromConnectionString(
            connectionString,
            ReceiveMode.PEEKLOCK
        );
    }

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.staging-enabled")
    public IMessageReceiver notificationsTestMessageReceiver(
        @Value("${queue.notifications.staging-read-connection-string}") String connectionString)
        throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageReceiverFromConnectionString(
            connectionString,
            ReceiveMode.PEEKLOCK
        );
    }
}
