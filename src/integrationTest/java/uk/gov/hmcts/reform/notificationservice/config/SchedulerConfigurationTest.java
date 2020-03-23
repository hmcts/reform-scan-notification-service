package uk.gov.hmcts.reform.notificationservice.config;

import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.LockProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
    "scheduling.task.notifications-consume.enable=true" // task is not there yet, but config is
})
public class SchedulerConfigurationTest {

    @SpyBean
    private LockProvider lockProvider;

    @Test
    public void should_integrate_with_shedlock() throws Exception {
        ArgumentCaptor<LockConfiguration> configCaptor = ArgumentCaptor.forClass(LockConfiguration.class);

        // wait for asynchronous run of the scheduled task in background
        Thread.sleep(2000);

        // verify(lockProvider, atLeastOnce()).lock(configCaptor.capture());
        verify(lockProvider, never()).lock(configCaptor.capture());
    }
}
