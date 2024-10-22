package uk.gov.hmcts.reform.notificationservice.config;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

//TODO: FACT-2026 - Whole config can go
@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key", havingValue = "false")
    public ServiceBusProcessorClient notificationMessageReceiver() {
        return mock(ServiceBusProcessorClient.class);
    }
}
