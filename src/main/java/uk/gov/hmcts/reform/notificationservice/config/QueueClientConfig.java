package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.ClientFactory;
import com.microsoft.azure.servicebus.IMessageReceiver;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class QueueClientConfig {

    private static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);

    private static String ENTITY_PATH = "EntityPath";

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.read-connection-string")
    public IMessageReceiver notificationsMessageReceiver(
        @Value("${queue.notifications.read-connection-string}") String connectionString)
        throws InterruptedException, ServiceBusException {
        logQueueName(connectionString);
        return ClientFactory.createMessageReceiverFromConnectionString(
            connectionString,
            ReceiveMode.PEEKLOCK
        );
    }

    private void logQueueName(String connectionString) {
        String[] split = connectionString.split(";");
        Arrays.stream(split)
            .filter(e -> e.startsWith(ENTITY_PATH))
            .findFirst()
            .ifPresent(c ->
                log.warn("Notification service connected to queue: {} ", c.replace(ENTITY_PATH, ""))
            );
    }

}