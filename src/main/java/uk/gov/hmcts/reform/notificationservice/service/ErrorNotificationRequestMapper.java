package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

@Component
public class ErrorNotificationRequestMapper {

    public ErrorNotificationRequest map(NotificationMsg notificationMsg) {
        return new ErrorNotificationRequest(
            notificationMsg.zipFileName,
            notificationMsg.poBox,
            notificationMsg.errorCode.name(),
            notificationMsg.errorDescription
        );
    }
}
