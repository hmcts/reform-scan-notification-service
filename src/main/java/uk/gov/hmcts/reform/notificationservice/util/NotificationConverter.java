package uk.gov.hmcts.reform.notificationservice.util;

import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsgRequest;
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


    public static NewNotification toNewNotification(NotificationMsgRequest notificationMsg, String client) {
        return new NewNotification(
            notificationMsg.zipFileName,
            notificationMsg.poBox,
            notificationMsg.container,
            notificationMsg.service,
            StringUtils.isEmpty(notificationMsg.documentControlNumber) ? "" : notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription,
            null,
            client
        );
    }
 }
