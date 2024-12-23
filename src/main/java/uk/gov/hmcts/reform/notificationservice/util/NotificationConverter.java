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
     * There are several fields of the request notification that are allowed to be null. To
     * avoid saving nulls to the database where we can, they are set to empty strings if null.
     * Message ID comes from the queue and are not present in API requests so this field is set as an
     * empty string.
     * @param notifyRequest the external request version of a notification
     * @param client the client that should be used to notify the supplier
     * @return a new notification for the database to save
     */
    public static NewNotification toNewNotification(NotifyRequest notifyRequest, String client) {
        return new NewNotification(
            notifyRequest.zipFileName,
            StringUtils.defaultIfEmpty(notifyRequest.poBox, ""),
            notifyRequest.container,
            notifyRequest.service,
            StringUtils.defaultIfEmpty(notifyRequest.documentControlNumber, ""),
            notifyRequest.errorCode,
            notifyRequest.errorDescription,
            "",
            StringUtils.defaultIfEmpty(client, "primary")
        );
    }
}
