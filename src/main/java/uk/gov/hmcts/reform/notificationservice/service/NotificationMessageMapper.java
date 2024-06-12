package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

/**
 * The `NotificationMessageMapper` class in Java maps specific fields from a `NotificationMsg` object to a
 * `NewNotification` object while adding a messageId for tracking and reference purposes.
 */
@Component
public class NotificationMessageMapper {

    /**
     * The function maps a NotificationMsg object to a NewNotification object by copying specific fields and adding a
     * messageId.
     *
     * @param notificationMsg The `map` method takes a `NotificationMsg` object and a `messageId` String
     *                        as parameters. It then creates a new `NewNotification` object using the values
     *                        from the `notificationMsg` object and the `messageId` parameter.
     * @param messageId The `messageId` parameter is a unique identifier for the notification message being
     *                  processed. It is used to associate the new notification with the original message
     *                  for tracking and reference purposes.
     * @return A new `NewNotification` object is being returned with the properties mapped from
     *      the `NotificationMsg` object `notificationMsg` and the `messageId` parameter.
     */
    public NewNotification map(NotificationMsg notificationMsg, String messageId) {
        return new NewNotification(
            notificationMsg.zipFileName,
            notificationMsg.poBox,
            notificationMsg.container,
            notificationMsg.service,
            notificationMsg.documentControlNumber,
            notificationMsg.errorCode,
            notificationMsg.errorDescription,
            messageId
        );
    }
}
