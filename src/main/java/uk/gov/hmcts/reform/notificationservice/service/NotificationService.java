package uk.gov.hmcts.reform.notificationservice.service;

import feign.FeignException;
import feign.FeignException.BadRequest;
import feign.FeignException.UnprocessableEntity;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClientSecondary;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationResponse;
import uk.gov.hmcts.reform.notificationservice.config.SecondaryClientJurisdictionsConfig;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.exception.FailedDependencyException;
import uk.gov.hmcts.reform.notificationservice.exception.NotFoundException;
import uk.gov.hmcts.reform.notificationservice.model.in.NotifyRequest;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;
import uk.gov.hmcts.reform.notificationservice.util.NotificationConverter;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class NotificationService {

    private static final Logger log = getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    private final ErrorNotificationClient notificationClient;
    private final ErrorNotificationClientSecondary notificationClientSecondary;

    private final String[] secondaryClientJurisdictions;

    public NotificationService(
            NotificationRepository notificationRepository,
            ErrorNotificationClient notificationClient,
            ErrorNotificationClientSecondary notificationClientSecondary,
            SecondaryClientJurisdictionsConfig secondaryClientJurisdictions
    ) {
        this.notificationRepository = notificationRepository;
        this.notificationClient = notificationClient;
        this.notificationClientSecondary = notificationClientSecondary;
        this.secondaryClientJurisdictions = secondaryClientJurisdictions.getJurisdictionList();;
    }

    public void processPendingNotifications() {
        List<Notification> notifications = notificationRepository.findPending();

        log.info("Number of notifications to process: {}", notifications.size());

        var okCount = 0;
        var failedCount = 0;
        var postponedCount = 0;

        for (var notification : notifications) {

            try {
                log.info("Sending error notification: {}", notification);
                ErrorNotificationResponse response = notification.client.equals("primary")
                    ? notificationClient.notify(mapToRequest(notification))
                    : notificationClientSecondary.notify(mapToRequest(notification));

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

    public List<Notification> getAllPendingNotifications() {
        return notificationRepository.findPending();
    }

    @Transactional(readOnly = true)
    public List<Notification> findByFileNameAndService(String fileName, String service) {
        return notificationRepository.find(fileName, service);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByDate(LocalDate date) {
        log.info("Getting notifications for date {}", date);
        return notificationRepository.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Notification> findByZipFileName(String zipFileName) {
        return notificationRepository.findByZipFileName(zipFileName);
    }

    /**
     * Finds a notification by its notification ID.
     * The notification entity is piped to a converter to map it into a class that represents
     * the notification's info.
     * @param notificationId the ID of the notification to be found
     * @return the info of the found notification
     * @throws NotFoundException if a notification with the given ID cannot be found
     */
    @Transactional(readOnly = true)
    public NotificationInfo findByNotificationId(Integer notificationId) {
        return notificationRepository.find(notificationId)
            .map(NotificationConverter::toNotificationResponse)
            .orElseThrow(() -> new NotFoundException("Notification not found with ID: " + notificationId));
    }

    /**
     * Saves a new notification request.
     * First identifies whether the notification should be sent via the primary or secondary client by looking at the
     * jurisdiction of the request. The request is then converted into the representation of a new notification for the
     * database which is then saved.
     * Then an attempt is made to notify the supplier which returns an ID. The saved notification is then updated with
     * this ID.
     * @param notifyRequest the notification information that should be saved to the database and sent to the supplier
     * @return the info of the saved notification
     * @throws FailedDependencyException if there is an issue trying to notify the supplier
     */
    @Transactional
    public NotificationInfo saveNotificationMsg(NotifyRequest notifyRequest) {
        String jurisdiction = Objects.requireNonNullElse(notifyRequest.jurisdiction, "").toLowerCase(Locale.ROOT);
        String client = Arrays.asList(secondaryClientJurisdictions).contains(jurisdiction) ? "secondary" : "primary";
        //Save notification as Pending
        NewNotification newNotificationForDb = NotificationConverter.toNewNotification(notifyRequest, client);
        Notification notificationFromDb = notificationRepository.save(newNotificationForDb);
        try {
            ErrorNotificationResponse response = newNotificationForDb.client.equals("primary")
                ? notificationClient.notify(mapToRequest(notificationFromDb))
                : notificationClientSecondary.notify(mapToRequest(notificationFromDb));
            //Update notification as Sent if Exela ok
            return NotificationConverter.toNotificationResponse(
                notificationRepository.updateNotificationStatusAsSent(
                    notificationFromDb.id, response.getNotificationId()));
        } catch (FeignException exception) {
            log.error("Error occurred trying to notify supplier. "
                          + "Updating notification status to fail. Notification ID: " + notificationFromDb.id);
            throw new FailedDependencyException(NotificationConverter
                                                    .toNotificationResponse(
                                                        notificationRepository.updateNotificationStatusAsFail(
                                                            notificationFromDb.id)), exception);
        }  catch (Exception e) {
            log.error("An unexpected error occurred trying to notify supplier. "
                          + "Updating notification status to fail. Notification ID: " + notificationFromDb.id);
            notificationRepository.updateNotificationStatusAsFail(notificationFromDb.id);
            throw e;
        }
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
            notification,
            exception.contentUTF8(),
            exception
        );

        notificationRepository.markAsFailure(notification.id);
    }

    private void postpone(Notification notification, FeignException exception) {
        log.error(
            "Received http status {} from client. Postponing notification for later. {}. Client response: {}",
            exception.status(),
            notification,
            exception.contentUTF8(),
            exception
        );
    }

    private void postpone(Notification notification, Exception exc) {
        log.error("Error processing pending notifications. {}", notification, exc);
    }
}
