package uk.gov.hmcts.reform.notificationservice.task;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import static org.slf4j.LoggerFactory.getLogger;

//TODO: FACT-2026 - whole class can go
@Component
@ConditionalOnProperty(
    prefix = "scheduling.task",
    name = PendingNotificationsTask.TASK_NAME + ".enabled",
    matchIfMissing = true
)
public class PendingNotificationsTask {

    private static final Logger log = getLogger(PendingNotificationsTask.class);
    public static final String TASK_NAME = "pending-notifications";

    private final NotificationService notificationService;

    public PendingNotificationsTask(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Scheduled(fixedDelayString = "${scheduling.task.pending-notifications.delay}") // see `values.yaml` in charts
    @SchedulerLock(name = TASK_NAME) // lock is needed so same notifications won't get through from different nodes
    public void run() {
        log.info("Started {} task", TASK_NAME);

        notificationService.processPendingNotifications();

        log.info("Finished {} task", TASK_NAME);
    }
}
