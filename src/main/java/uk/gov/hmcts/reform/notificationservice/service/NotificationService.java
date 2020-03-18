package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;

import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class NotificationService {

    private static final Logger log = getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(
        NotificationRepository notificationRepository
    ) {
        this.notificationRepository = notificationRepository;
    }

    public void processPendingNotifications() {
        List<Notification> notifications = notificationRepository.findPending();

        log.info("Notifications to process: {}", notifications.size());
    }
}
