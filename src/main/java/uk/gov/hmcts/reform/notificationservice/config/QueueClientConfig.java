package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.IQueueClient;
import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class QueueClientConfig {

    @Bean
    public IQueueClient readNotificationsQueueClient(
        @Value("${queue.notifications.read-connection-string}") String connectionString,
        @Value("${queue.notifications.queue-name}") String queueName
    ) throws InterruptedException, ServiceBusException {
        return new QueueClient(
            new ConnectionStringBuilder(connectionString, queueName),
            ReceiveMode.PEEKLOCK
        );
    }

}
