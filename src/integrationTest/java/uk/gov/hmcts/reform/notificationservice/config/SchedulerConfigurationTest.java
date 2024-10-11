package uk.gov.hmcts.reform.notificationservice.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.gov.hmcts.reform.notificationservice.task.PendingNotificationsTask;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

//TODO: FACT-2026 - Whole class can go
@SpringBootTest(properties = {
    // consume task is not under shedlock. including config for future in case it will be
    "scheduling.task.notifications-consume.enable=true",
    "scheduling.task.pending-notifications.delay=500", // in ms
    "scheduling.task.pending-notifications.enabled=true"
})
public class SchedulerConfigurationTest {

    @SpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // wait for asynchronous run of the scheduled task in background
        Thread.sleep(2000);

        verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        assertThat(configCaptor.getAllValues())
            .extracting(LockConfiguration::getName)
            .containsOnly(
                PendingNotificationsTask.TASK_NAME
            );
    }
}
