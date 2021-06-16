package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
@ConditionalOnProperty(value = "scheduling.task.notifications-consume.enabled", matchIfMissing = true)
public class NotificationMessageProcessTask {

    private final ServiceBusProcessorClient serviceBusProcessorClient;

    public NotificationMessageProcessTask(
        ServiceBusProcessorClient serviceBusProcessorClient
    ) {
        this.serviceBusProcessorClient = serviceBusProcessorClient;
    }

    @PostConstruct
    void startProcessor() {
        serviceBusProcessorClient.start();
    }
}
