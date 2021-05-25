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

import java.time.LocalDate;
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

        int okCount = 0;
        int failedCount = 0;
        int postponedCount = 0;

        for (var notification : notifications) {
            log.info("Sending error notification. {}", notification.basicInfo());

            try {
                ErrorNotificationResponse response = notificationClient.notify(mapToRequest(notification));

                notificationRepository.markAsSent(notification.id, response.getNotificationId());

                log.info(
                    "Error notification sent. {}. Notification ID: {}",
                    notification.basicInfo(),
                    response.getNotificationId()
                );
                okCount++;

            } catch (BadRequest | UnprocessableEntity exception) {
                fail(notification, exception);
                failedCount++;

            } catch (FeignException exception) {
                postpone(notification, exception);
                postponedCount++;
            } catch (Exception e) {
                postpone(notification, e);
                postponedCount++;
            }
        }

        log.info(
            "Finished sending notifications. OK: {}, Failed: {}, Postponed: {}",
            okCount,
            failedCount,
            postponedCount
        );
    }

    @Transactional(readOnly = true)
    public List<Notification> findByFileNameAndService(String fileName, String service) {
        final String fileNameCleanedUp = fileName.replaceAll("[\n|\r|\t]", "");
        log.info("Getting notifications for file {}, service {}", fileNameCleanedUp, service);

        return notificationRepository.find(fileNameCleanedUp, service);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByDate(LocalDate date) {
        log.info("Getting notifications for date {}", date);
        return notificationRepository.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByZipFileName(String zipFileName) {
        log.info("Getting notifications for zip file name");
        return notificationRepository.findByZipFileName(zipFileName);
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

    private void fail(Notification notification, FeignException.FeignClientException exception) {
        log.error(
            "Received http status {} from client. Marking as failure. {}. Client response: {}",
            exception.status(),
            notification.basicInfo(),
            exception.contentUTF8(),
            exception
        );

        notificationRepository.markAsFailure(notification.id);
    }

    private void postpone(Notification notification, FeignException exception) {
        log.error(
            "Received http status {} from client. Postponing notification for later. {}. Client response: {}",
            exception.status(),
            notification.basicInfo(),
            exception.contentUTF8(),
            exception
        );
    }

    private void postpone(Notification notification, Exception exc) {
        log.error("Error processing pending notifications. {}", notification.basicInfo(), exc);
    }
}
