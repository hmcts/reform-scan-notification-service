package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

@Component
public class NotificationMessageMapper {

    public NewNotification map(NotificationMsg notificationMsg, String messageId, String client) {
        return new NewNotification(
            notificationMsg.zipFileName,
            notificationMsg.poBox,
            notificationMsg.container,
            notificationMsg.service,
            notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription,
            messageId,
            client
        );
    }
}
