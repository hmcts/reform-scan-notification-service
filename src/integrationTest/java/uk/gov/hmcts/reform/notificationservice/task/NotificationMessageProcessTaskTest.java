package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.Mockito.verify;

//TODO: FACT-2026 - whole class can go
@SpringBootTest(properties = {"scheduling.task.notifications-consume.enable=true"})
public class NotificationMessageProcessTaskTest {

    @SpyBean
    private NotificationMessageProcessTask notificationMessageProcessTask;

    @MockBean
    private ServiceBusProcessorClient serviceBusProcessorClient;

    @Test
    public void should_start_ServiceBusProcessorClient() {
        verify(serviceBusProcessorClient).start();
    }
}
