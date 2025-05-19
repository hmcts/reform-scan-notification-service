package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.verify;

//TODO: FACT-2026 - whole class can go
@SpringBootTest(properties = {"scheduling.task.notifications-consume.enable=true"})
public class NotificationMessageProcessTaskTest {

    @MockitoSpyBean
    private NotificationMessageProcessTask notificationMessageProcessTask;

    @MockitoBean
    private ServiceBusProcessorClient serviceBusProcessorClient;

    @Test
    public void should_start_ServiceBusProcessorClient() {
        verify(serviceBusProcessorClient).start();
    }
}
