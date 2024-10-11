package uk.gov.hmcts.reform.notificationservice.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.notificationservice.service.NotificationMessageProcessor;

//TODO: FACT-2026 - whole class can go
@Configuration
@ConditionalOnExpression("!${jms.enabled}")
public class QueueClientConfig {

    private static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);

    @Bean
    @ConditionalOnProperty(name = "queue.notifications.access-key")
    public ServiceBusProcessorClient notificationsMessageReceiver(
        @Value("${queue.notifications.access-key}") String accessKey,
        @Value("${queue.notifications.access-key-name}") String accessKeyName,
        @Value("${queue.notifications.name}") String queueName,
        @Value("${queue.notifications.namespace}") String namespace,
        NotificationMessageProcessor notificationMessageProcessor
    ) {

        String connectionString  = String.format(
            "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            namespace,
            accessKeyName,
            accessKey
        );

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .processor()
            .queueName(queueName)
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .disableAutoComplete()
            .processMessage(notificationMessageProcessor::processNextMessage)
            .processError(c -> log.error("Notification queue handle error {}", c.getErrorSource(), c.getException()))
            .buildProcessorClient();
    }

}
