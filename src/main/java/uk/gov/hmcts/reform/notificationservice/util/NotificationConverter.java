package uk.gov.hmcts.reform.notificationservice.util;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.model.in.NotifyRequest;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;

import static java.lang.Math.min;

/**
 * Utility for converting between the various notification models.
 */
public final class NotificationConverter {

    private static final int MAX_ERROR_DESCRIPTION_LENGTH = 1024;

    private NotificationConverter() {}

    /**
     * Maps a database representation notification to a response notification.
     * @param notification a notification from database
     * @return notification for use in an API response
     */
    public static NotificationInfo toNotificationResponse(Notification notification) {
        return new NotificationInfo(
            notification.id,
            notification.confirmationId,
            notification.zipFileName,
            notification.poBox,
            notification.container,
            notification.service,
            notification.documentControlNumber,
            notification.errorCode.name(),
            notification.errorDescription == null
                ?
                ""
                :
                notification.errorDescription.substring(
                    0,
                    min(MAX_ERROR_DESCRIPTION_LENGTH, notification.errorDescription.length())
                ),
            notification.createdAt,
            notification.processedAt,
            notification.status.name()
        );
    }

    /**
     * Maps an API request notification to a notification for the database to save.
     * @param notifyRequest the external request version of a notification
     * @param client the client that should be used to notify the supplier
     * @return a new notification for the database to save
     */
    public static NewNotification toNewNotification(NotifyRequest notifyRequest, String client) {
        return new NewNotification(
            notifyRequest.zipFileName,
            notifyRequest.poBox,
            notifyRequest.container,
            notifyRequest.service,
            StringUtils.isEmpty(notifyRequest.documentControlNumber) ? "" : notifyRequest.documentControlNumber,
            notifyRequest.errorCode,
            notifyRequest.errorDescription,
            null,
            client
        );
    }
}
