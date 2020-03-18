package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationClient;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

@Service
public class NotificationMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageHandler.class);

    private final ErrorNotificationRequestMapper errorNotificationRequestMapper;
    private final ErrorNotificationClient errorNotificationClient;

    public NotificationMessageHandler(
        ErrorNotificationRequestMapper errorNotificationRequestMapper,
        ErrorNotificationClient errorNotificationClient
    ) {
        this.errorNotificationRequestMapper = errorNotificationRequestMapper;
        this.errorNotificationClient = errorNotificationClient;
    }


    public void handleNotificationMessage(NotificationMsg notificationMsg) {
        log.info("Handle notification message, Zip File: {}", notificationMsg.zipFileName);

        var request = errorNotificationRequestMapper.map(notificationMsg);

        var response = errorNotificationClient.notify(request);

        log.info("Handle notification message success, Zip File: {}, Response Notification ID: {}",
            notificationMsg.zipFileName, response.getNotificationId());

    }
}
