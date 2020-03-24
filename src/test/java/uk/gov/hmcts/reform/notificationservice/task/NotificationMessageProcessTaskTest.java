package uk.gov.hmcts.reform.notificationservice.task;

import com.microsoft.azure.servicebus.primitives.ServiceBusException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.notificationservice.service.NotificationMessageProcessor;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NotificationMessageProcessTaskTest {

    private NotificationMessageProcessTask notificationMessageProcessTask;

    @Mock
    private NotificationMessageProcessor notificationMessageProcessor;

    @BeforeEach
    public void setUp() {
        notificationMessageProcessTask = new NotificationMessageProcessTask(notificationMessageProcessor);
    }

    @Test
    public void consumeMessages_processes_messages_until_envelope_processor_returns_false() throws Exception {
        // given
        given(notificationMessageProcessor.processNextMessage()).willReturn(true, true, true, false);

        // when
        notificationMessageProcessTask.consumeMessages();

        // then
        verify(notificationMessageProcessor, times(4)).processNextMessage();
    }

    @Test
    public void consumeMessages_stops_processing_when_envelope_processor_throws_exception() throws Exception {
        // given
        willThrow(new ServiceBusException(true)).given(notificationMessageProcessor).processNextMessage();

        // when
        notificationMessageProcessTask.consumeMessages();

        // then
        verify(notificationMessageProcessor, times(1)).processNextMessage();
    }

    @Test
    public void consumeMessages_stops_processing_when_envelope_processor_throws_interrupted_exception()
        throws Exception {
        // given
        willThrow(new InterruptedException()).given(notificationMessageProcessor).processNextMessage();

        // when
        notificationMessageProcessTask.consumeMessages();

        // then
        verify(notificationMessageProcessor, times(1)).processNextMessage();
    }
}