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

/**
 * The `QueueClientConfig` class in Java configures a Service Bus processor client for receiving
 * messages from a specified Azure Service Bus queue based on provided access key and access key name.
 */
@Configuration
@ConditionalOnExpression("!${jms.enabled}")
public class QueueClientConfig {

    private static final Logger log = LoggerFactory.getLogger(QueueClientConfig.class);

    /**
     * The function `notificationsMessageReceiver` creates a Service Bus processor client for receiving messages from a
     * specified queue using the provided access key and access key name.
     *
     * @param accessKey The `accessKey` parameter in the code snippet represents the access key required to
     *                  authenticate and authorize access to the Azure Service Bus queue. This key is used along
     *                  with the access key name and namespace to construct the connection string for the Service
     *                  Bus client.
     * @param accessKeyName The `accessKeyName` parameter in the code snippet refers to the name of the Shared
     *                      Access Key that is used for authentication when connecting to the Azure Service Bus
     *                      queue. This key is part of the connection string that is constructed to establish a
     *                      connection to the Service Bus namespace.
     * @param queueName The `queueName` parameter in the code snippet refers to the name of the Service Bus queue
     *                  from which messages will be received. It is used to specify the specific queue that the
     *                  `ServiceBusProcessorClient` will be listening to for incoming messages.
     * @param namespace The `namespace` parameter in the code snippet refers to the Azure Service Bus namespace
     *                  where the queue is located. It is part of the connection string used to connect to the
     *                  Service Bus queue for receiving messages.
     * @param notificationMessageProcessor The `notificationMessageProcessor` parameter in
     *                                     the `notificationsMessageReceiver` method is an instance of the
     *                                     `NotificationMessageProcessor` class. This parameter is used to process
     *                                     the incoming messages from the notification queue.
     * @return A ServiceBusProcessorClient bean is being returned.
     */
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
