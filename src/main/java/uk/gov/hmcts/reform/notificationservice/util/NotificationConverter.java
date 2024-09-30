package uk.gov.hmcts.reform.notificationservice.util;

import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;

import static java.lang.Math.min;

public final class NotificationConverter {

    private static final int MAX_ERROR_DESCRIPTION_LENGTH = 1024;

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
}
