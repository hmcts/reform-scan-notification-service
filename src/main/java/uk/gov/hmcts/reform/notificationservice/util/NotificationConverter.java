package uk.gov.hmcts.reform.notificationservice.util;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationEntity;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsgRequest;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;

import java.time.Instant;

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

    public static NotificationEntity toNotificationEntity(NotificationMsgRequest notificationMsg) {
        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setZipFileName(notificationEntity.getZipFileName());
        notificationEntity.setPoBox(notificationMsg.getPoBox());
        notificationEntity.setContainer(notificationMsg.getContainer());
        notificationEntity.setService(notificationMsg.getService());
        notificationEntity.setDocumentControlNumber(notificationMsg.getDocumentControlNumber());
        notificationEntity.setErrorCode(notificationMsg.getErrorCode());
        notificationEntity.setErrorDescription(notificationMsg.getErrorDescription());
        notificationEntity.setCreatedAt(Instant.now());
        notificationEntity.setCreatedAt(Instant.now());
        notificationEntity.setMessageId(RandomStringUtils.random(10));
        notificationEntity.setClient("primary");
        return notificationEntity;
    }

    public static NotificationInfo toNotificationResponse(NotificationEntity notification) {
        return new NotificationInfo(
            notification.getId(),
            notification.getConfirmationId(),
            notification.getZipFileName(),
            notification.getPoBox(),
            notification.getContainer(),
            notification.getService(),
            notification.getDocumentControlNumber(),
            notification.getErrorCode().name(),
            notification.getErrorDescription() == null
                ?
                ""
                :
                notification.getErrorDescription().substring(
                    0,
                    min(MAX_ERROR_DESCRIPTION_LENGTH, notification.getErrorDescription().length())
                ),
            notification.getCreatedAt(),
            notification.getProcessedAt(),
            notification.getStatus().name()
        );
    }
 }
