package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationResponse;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

@Service
public class NotificationService {

    private static final Logger log = getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final ErrorNotificationClient notificationClient;

    public NotificationService(
        NotificationRepository notificationRepository,
        ErrorNotificationClient notificationClient
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationClient = notificationClient;
    }

    public void processPendingNotifications() {
        List<Notification> notifications = notificationRepository.findPending();

        log.info("Notifications to process: {}", notifications.size());

        notifications.forEach(this::processNotifications);
    }

    public List<NotificationResponse> findNotificationsByFileNameAndService(String fileName, String service) {
        log.info("Getting notifications for file {}, service {}", fileName, service);

        return notificationRepository.find(fileName, service)
            .stream()
            .map(this::mapToNotificationResponse)
            .collect(toList());
    }

    private ErrorNotificationRequest mapToRequest(Notification notification) {
        return new ErrorNotificationRequest(
            notification.zipFileName,
            notification.poBox,
            notification.errorCode.name(),
            notification.errorDescription,
            String.valueOf(notification.id)
        );
    }

    private void processNotifications(Notification notification) {
        ErrorNotificationResponse response = notificationClient.notify(mapToRequest(notification));

        notificationRepository.markAsSent(notification.id, response.getNotificationId());

        log.info(
            "Error notification sent. Service: {}, Zip file: {}, ID: {}, Notification ID: {}",
            notification.service,
            notification.zipFileName,
            notification.id,
            response.getNotificationId()
        );
    }

    private NotificationResponse mapToNotificationResponse(Notification notification) {
        return new NotificationResponse(
            notification.notificationId,
            notification.zipFileName,
            notification.poBox,
            notification.service,
            notification.documentControlNumber,
            notification.errorCode.name(),
            notification.createdAt,
            notification.processedAt,
            notification.status.name()
        );
    }
}
