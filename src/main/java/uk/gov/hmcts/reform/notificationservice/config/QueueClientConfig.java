package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QueueClientConfig {

    private static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key")
    public IMessageReceiver notificationsMessageReceiver(
        @Value("${queue.notifications.access-key}") String accessKey,
        @Value("${queue.notifications.access-key-name}") String accessKeyName,
        @Value("${queue.notifications.name}") String queueName,
        @Value("${queue.notifications.namespace}") String namespace
    ) throws InterruptedException, ServiceBusException {
        return ClientFactory.createMessageReceiverFromConnectionString(
            new ConnectionStringBuilder(namespace, queueName, accessKeyName, accessKey).toString(),
            ReceiveMode.PEEKLOCK
        );
    }

}
