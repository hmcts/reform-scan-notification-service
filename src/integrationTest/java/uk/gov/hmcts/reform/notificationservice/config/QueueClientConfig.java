package uk.gov.hmcts.reform.notificationservice.config;

import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key", havingValue = "false")
    public ServiceBusReceiverClient notificationMessageReceiver() {
        return mock(ServiceBusReceiverClient.class);
    }
}
