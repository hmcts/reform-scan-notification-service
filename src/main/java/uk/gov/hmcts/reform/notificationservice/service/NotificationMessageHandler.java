package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

@Service
public class NotificationMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageHandler.class);

    private final NotificationMapper notificationMapper;
    private final NotificationRepository notificationRepository;

    public NotificationMessageHandler(
        NotificationMapper notificationMapper,
        NotificationRepository notificationRepository
    ) {
        this.notificationMapper = notificationMapper;
        this.notificationRepository = notificationRepository;
    }


    public void handleNotificationMessage(NotificationMsg notificationMsg) {
        log.info("Handle notification message, Zip File: {}", notificationMsg.zipFileName);

        var newNotification = notificationMapper.map(notificationMsg);

        long id = notificationRepository.insert(newNotification);
        log.info(
            "Handle notification message successful, Zip File: {}, Inserted Notification ID: {}",
            notificationMsg.zipFileName,
            id
        );

    }
}
