package uk.gov.hmcts.reform.notificationservice.config;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
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
    public ServiceBusReceiverClient notificationsMessageReceiver(
        @Value("${queue.notifications.access-key}") String accessKey,
        @Value("${queue.notifications.access-key-name}") String accessKeyName,
        @Value("${queue.notifications.name}") String queueName,
        @Value("${queue.notifications.namespace}") String namespace
    ) {


        String connectionString  = String.format(
            "Endpoint=sb://%s;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            namespace,
            accessKeyName,
            accessKey
        );


        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .receiver()
            .queueName(queueName)
            .receiveMode(ServiceBusReceiveMode.PEEK_LOCK)
            .disableAutoComplete()
            .buildClient();
    }

}
