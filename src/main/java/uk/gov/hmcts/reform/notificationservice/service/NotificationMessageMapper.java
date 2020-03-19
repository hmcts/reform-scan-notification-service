package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

@Component
public class NotificationMessageMapper {

    public NewNotification map(NotificationMsg notificationMsg) {
        return new NewNotification(
            notificationMsg.zipFileName,
            notificationMsg.poBox,
            notificationMsg.service,
            notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription
        );
    }
}
