package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueClientConfig {

    @Bean
    public IMessageReceiver notificationsMessageReceiver(
        @Value("${queue.notifications.read-connection-string}") String connectionString)
        throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageReceiverFromConnectionString(
            connectionString,
            ReceiveMode.PEEKLOCK
        );
    }

}
