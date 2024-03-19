package uk.gov.hmcts.reform.notificationservice;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

final class Configuration {

    private static final Config CONFIG = ConfigFactory.load();

    static final String TEST_URL = CONFIG.getString("test-url");
    static final String TEST_S2S_SECRET = CONFIG.getString("test-s2s-secret");
    static final String TEST_S2S_URL = CONFIG.getString("test-s2s-url");
    static final String NOTIFICATION_QUEUE_ACCESS_KEY_WRITE = CONFIG
        .getString("test-notification-queue-access-key-write");
    static final String NOTIFICATION_QUEUE_ACCESS_KEY_NAME_WRITE = CONFIG
        .getString("test-notification-queue-access-key-name-write");
    static final String NOTIFICATION_QUEUE_NAME = CONFIG
        .getString("test-notification-queue-name");
    static final String NOTIFICATION_QUEUE_NAMESPACE = CONFIG
        .getString("test-notification-queue-namespace");

    private Configuration() {
        // utility class construct
    }

    /**
     * The function `getSendClient` creates and returns a ServiceBusSenderClient for sending messages
     * to a specified queue using the provided connection string and queue name.
     *
     * @return A `ServiceBusSenderClient` object is being returned.
     */
    static ServiceBusSenderClient getSendClient() {
        String connectionString = String.format(
            "Endpoint=sb://%s.servicebus.windows.net;SharedAccessKeyName=%s;SharedAccessKey=%s;",
            NOTIFICATION_QUEUE_NAMESPACE,
            NOTIFICATION_QUEUE_ACCESS_KEY_NAME_WRITE,
            NOTIFICATION_QUEUE_ACCESS_KEY_WRITE
        );

        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(NOTIFICATION_QUEUE_NAME)
            .buildClient();
    }
}
