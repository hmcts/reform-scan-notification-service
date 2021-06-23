package uk.gov.hmcts.reform.notificationservice.task;

import com.azure.messaging.servicebus.ServiceBusProcessorClient;
import io.github.netmikey.logunit.api.LogCapturer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.slf4j.event.Level.ERROR;
import static org.slf4j.event.Level.INFO;

@ExtendWith(MockitoExtension.class)
class NotificationMessageProcessTaskTest {

    @RegisterExtension
    public LogCapturer logs = LogCapturer.create().captureForType(NotificationMessageProcessTask.class);

    @InjectMocks
    private NotificationMessageProcessTask notificationMessageProcessTask;

    @Mock
    private ServiceBusProcessorClient serviceBusProcessorClient;

    @Test
    public void should_log_when_listener_is_not_working() {
        given(serviceBusProcessorClient.isRunning()).willReturn(false);
        notificationMessageProcessTask.checkServiceBusProcessorClient();
        assertThat(
            logs.assertContains(event -> event.getLevel() == ERROR, "Error level log not found").getMessage())
            .isEqualTo("Notification queue consume listener is NOT running!!!");
    }

    @Test
    public void should_log_when_listener_is_working() {
        given(serviceBusProcessorClient.isRunning()).willReturn(true);
        notificationMessageProcessTask.checkServiceBusProcessorClient();
        assertThat(
            logs.assertContains(event -> event.getLevel() == INFO, "Info level log not found").getMessage())
            .isEqualTo("Notification queue consume listener is working.");
    }
}
