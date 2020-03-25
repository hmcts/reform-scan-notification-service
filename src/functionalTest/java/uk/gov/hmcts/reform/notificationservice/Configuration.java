package uk.gov.hmcts.reform.notificationservice;

import com.microsoft.azure.servicebus.QueueClient;
import com.microsoft.azure.servicebus.ReceiveMode;
import com.microsoft.azure.servicebus.primitives.ConnectionStringBuilder;
import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

final class Configuration {

    private static final Config CONFIG = ConfigFactory.load();

    private static final String ERROR_MESSAGE = "Failure building queue client";

    static final String TEST_URL = CONFIG.getString("test-url");
    static final String TEST_S2S_SECRET = CONFIG.getString("test-s2s-secret");
    static final String TEST_S2S_URL = CONFIG.getString("test-s2s-url");
    static final String NOTIFICATION_QUEUE_CONN_STRING_READ = CONFIG
        .getString("test-notification-queue-connection-string-read");
    static final String NOTIFICATION_QUEUE_CONN_STRING_WRITE = CONFIG
        .getString("test-notification-queue-connection-string-write");

    private Configuration() {
        // utility class construct
    }

    static QueueClient getSendClient() {
        try {
            return new QueueClient(
                new ConnectionStringBuilder(NOTIFICATION_QUEUE_CONN_STRING_WRITE),
                ReceiveMode.PEEKLOCK
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(ERROR_MESSAGE, exception);
        } catch (ServiceBusException exception) {
            throw new RuntimeException(ERROR_MESSAGE, exception);
        }
    }
}
