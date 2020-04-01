package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import feign.FeignException.BadRequest;
import feign.FeignException.UnprocessableEntity;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;

import java.util.List;

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

    @Transactional(readOnly = true)
    public List<Notification> findByFileNameAndService(String fileName, String service) {
        final String fileNameCleanedUp = fileName.replaceAll("[\n|\r|\t]", "");
        log.info("Getting notifications for file {}, service {}", fileNameCleanedUp, service);

        return notificationRepository.find(fileNameCleanedUp, service);
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
        try {
            ErrorNotificationResponse response = notificationClient.notify(mapToRequest(notification));

            notificationRepository.markAsSent(notification.id, response.getNotificationId());

            log.info(
                "Error notification sent. {}. Notification ID: {}",
                notification.basicInfo(),
                response.getNotificationId()
            );
        } catch (BadRequest | UnprocessableEntity exception) {
            log.error(
                "Received http status {} from client. Marking as failure. {}. Client response: {}",
                exception.status(),
                notification.basicInfo(),
                exception.contentUTF8(),
                exception
            );

            notificationRepository.markAsFailure(notification.id);

        } catch (FeignException exception) {
            log.error(
                "Received http status {} from client. Postponing notification for later. {}. Client response: {}",
                exception.status(),
                notification.basicInfo(),
                exception.contentUTF8(),
                exception
            );
        }
    }
}
