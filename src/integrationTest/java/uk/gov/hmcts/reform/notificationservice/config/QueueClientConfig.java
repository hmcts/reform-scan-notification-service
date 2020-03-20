package uk.gov.hmcts.reform.notificationservice.config;

import com.microsoft.azure.servicebus.IMessageReceiver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.mockito.Mockito.mock;

@Configuration
@Profile("integrationTest")
public class QueueClientConfig {
    @Bean
    public IMessageReceiver notificationMessageReceiver() {
        return mock(IMessageReceiver.class);
    }
}
