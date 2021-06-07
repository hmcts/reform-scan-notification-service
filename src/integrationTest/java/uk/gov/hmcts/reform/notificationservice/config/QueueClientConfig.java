package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.IMessageReceiver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.mockito.Mockito.mock;

@Configuration
public class QueueClientConfig {

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key", havingValue = "false")
    public IMessageReceiver notificationMessageReceiver() {
        return mock(IMessageReceiver.class);
    }
}
