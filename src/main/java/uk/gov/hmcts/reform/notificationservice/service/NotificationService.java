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

/**
 * The `NotificationService` class in Java handles processing and managing notifications, including sending error
 * notifications, retrieving notifications based on various criteria, and handling exceptions during notification
 * processing.
 */
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

        log.info("Number of notifications to process: {}", notifications.size());

        var okCount = 0;
        var failedCount = 0;
        var postponedCount = 0;

        for (var notification : notifications) {
            log.info("Sending error notification. {}", notification);

            try {
                ErrorNotificationResponse response = notificationClient.notify(mapToRequest(notification));

                notificationRepository.markAsSent(notification.id, response.getNotificationId());

                log.info(
                    "Error notification sent. {}. Notification ID: {}",
                    notification,
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

    /**
     * The function `getAllPendingNotifications` returns a list of pending notifications by querying the notification
     * repository.
     *
     * @return A List of Notification objects that are pending.
     */
    public List<Notification> getAllPendingNotifications() {
        return notificationRepository.findPending();
    }

    /**
     * This function retrieves a list of notifications based on the provided file name and service.
     *
     * @param fileName The `fileName` parameter is a string that represents the name of the file you
     *                 want to search for in the notifications.
     * @param service Service is a parameter that represents the type of service for which notifications
     *                are being searched. It could be a specific service name or identifier that
     *                helps in filtering notifications based on the service they belong to.
     * @return A list of notifications that match the given file name and service.
     */
    @Transactional(readOnly = true)
    public List<Notification> findByFileNameAndService(String fileName, String service) {
        return notificationRepository.find(fileName, service);
    }

    /**
     * This Java function retrieves notifications for a specific date from a repository in a read-only transactional
     * context.
     *
     * @param date The `date` parameter is of type `LocalDate`, which represents a date without a
     *             time zone in the ISO-8601 calendar system, such as `2022-01-31`.
     * @return A list of notifications for the specified date is being returned.
     */
    @Transactional(readOnly = true)
    public List<Notification> findByDate(LocalDate date) {
        log.info("Getting notifications for date {}", date);
        return notificationRepository.findByDate(date);
    }

    /**
     * This function retrieves a list of notifications based on a given zip file name
     * in a read-only transactional context.
     *
     * @param zipFileName The `zipFileName` parameter is a string that represents the name of
     *                    a ZIP file. The `findByZipFileName` method is used to retrieve a list of
     *                    notifications associated with the specified ZIP file name.
     * @return A list of notifications with the specified zip file name is being returned.
     */
    @Transactional(readOnly = true)
    public List<Notification> findByZipFileName(String zipFileName) {
        return notificationRepository.findByZipFileName(zipFileName);
    }

    /**
     * The function `mapToRequest` converts a `Notification` object to an `ErrorNotificationRequest` object by mapping
     * specific fields.
     *
     * @param notification The `mapToRequest` method takes a `Notification` object as a parameter and
     *                     maps its properties to an `ErrorNotificationRequest` object.
     * @return An `ErrorNotificationRequest` object is being returned.
     */
    private ErrorNotificationRequest mapToRequest(Notification notification) {
        return new ErrorNotificationRequest(
            notification.zipFileName,
            notification.poBox,
            notification.errorCode.name(),
            notification.errorDescription,
            String.valueOf(notification.id)
        );
    }

    /**
     * This function logs an error message with details of a FeignClientException and marks a
     * notification as failure in a repository.
     *
     * @param notification The `notification` parameter is an object representing a notification that was
     *                     being processed when the failure occurred. It likely contains information such as
     *                     the notification ID, content, recipient, timestamp, etc.
     * @param exception The `exception` parameter in the `fail` method is of
     *                  type `FeignException.FeignClientException`. It represents an exception that occurs when
     *                  a Feign client encounters an error while making a request to a remote server.
     */
    private void fail(Notification notification, FeignException.FeignClientException exception) {
        log.error(
            "Received http status {} from client. Marking as failure. {}. Client response: {}",
            exception.status(),
            notification,
            exception.contentUTF8(),
            exception
        );

        notificationRepository.markAsFailure(notification.id);
    }

    /**
     * The function `postpone` logs an error message with details about a FeignException and a Notification object.
     *
     * @param notification The `notification` parameter is an object representing a notification that
     *                     needs to be postponed due to a FeignException.
     * @param exception The `exception` parameter in the `postpone` method is of type `FeignException`. It is
     *                  used to capture any exceptions that occur during a Feign client request, such
     *                  as HTTP status codes, response content, and other relevant information.
     */
    private void postpone(Notification notification, FeignException exception) {
        log.error(
            "Received http status {} from client. Postponing notification for later. {}. Client response: {}",
            exception.status(),
            notification,
            exception.contentUTF8(),
            exception
        );
    }

    /**
     * The function "postpone" logs an error message when processing pending notifications encounters an exception.
     *
     * @param notification The `notification` parameter is an object representing a notification that
     *                     needs to be processed.
     * @param exc The `exc` parameter in the `postpone` method is of type `Exception`. It is used to pass
     *            an exception object that occurred during the processing of pending notifications. This
     *            allows the method to log the error message along with the exception details for debugging
     *            and error handling purposes.
     */
    private void postpone(Notification notification, Exception exc) {
        log.error("Error processing pending notifications. {}", notification, exc);
    }
}
