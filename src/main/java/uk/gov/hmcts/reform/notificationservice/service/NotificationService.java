package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
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

    private ErrorNotificationRequest mapToRequest(Notification notification) {
        return new ErrorNotificationRequest(
            notification.zipFileName,
            notification.poBox,
            notification.errorCode.name(),
            notification.errorDescription,
            String.valueOf(notification.id)
        );
    }

    @Transactional
    public void processNotifications(Notification notification) {
        try {
            ErrorNotificationResponse response = notificationClient.notify(mapToRequest(notification));

            notificationRepository.markAsSent(notification.id, response.getNotificationId());

            log.info(
                "Error notification sent. Service: {}, Zip file: {}, ID: {}, Notification ID: {}",
                notification.service,
                notification.zipFileName,
                notification.id,
                response.getNotificationId()
            );
        } catch (FeignException.BadRequest | FeignException.UnprocessableEntity exception) {
            logFeignError(
                "Received {} from client. Marking as failure. Service: {}, Zip file: {}, ID: {}, Client response: {}",
                notification,
                exception
            );

            notificationRepository.markAsFailure(notification.id);
        } catch (FeignException exception) {
            logFeignError(
                "Received {} from client. Postponing notification for later. "
                    + "Service: {}, Zip file: {}, ID: {}, Client response: {}",
                notification,
                exception
            );
        }
    }

    private void logFeignError(String messagePattern, Notification notification, FeignException exception) {
        var status = HttpStatus.valueOf(exception.status());

        log.error(
            messagePattern,
            status.getReasonPhrase(),
            notification.service,
            notification.zipFileName,
            notification.id,
            exception.contentUTF8(),
            exception
        );
    }
}
