package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

/**
 * The `NotificationMessageHandler` class in Java handles notification messages by logging information, mapping to a new
 * notification, inserting it into a repository, and logging successful handling.
 */
@Service
public class NotificationMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageHandler.class);

    private final NotificationMessageMapper notificationMessageMapper;
    private final NotificationRepository notificationRepository;

    public NotificationMessageHandler(
        NotificationMessageMapper notificationMessageMapper,
        NotificationRepository notificationRepository
    ) {
        this.notificationMessageMapper = notificationMessageMapper;
        this.notificationRepository = notificationRepository;
    }

    /**
     * The `handleNotificationMessage` function logs information about a notification message, maps it to a new
     * notification, inserts it into a repository, and logs the successful handling of the message.
     *
     * @param notificationMsg The `notificationMsg` parameter is an object of type `NotificationMsg` which
     *                        contains information about a notification message. In the provided code
     *                        snippet, it is used to log the zip file name and then map its contents to a new
     *                        notification object.
     * @param messageId The `messageId` parameter in the `handleNotificationMessage` method is a `String` type.
     *                  It is used to uniquely identify the message being processed. This identifier can
     *                  be used for tracking and logging purposes to associate the notification message
     *                  with a specific ID.
     */
    public void handleNotificationMessage(NotificationMsg notificationMsg, String messageId) {
        log.info("Handle notification message, Zip File: {}", notificationMsg.zipFileName);

        var newNotification = notificationMessageMapper.map(notificationMsg, messageId);

        long id = notificationRepository.insert(newNotification);
        log.info(
            "Handle notification message successful, Zip File: {}, Inserted Notification ID: {}",
            notificationMsg.zipFileName,
            id
        );
    }
}
