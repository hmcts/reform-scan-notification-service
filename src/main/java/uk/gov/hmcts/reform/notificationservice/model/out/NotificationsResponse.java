package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class NotificationsResponse {
    @JsonProperty("count")
    public final int count;

    @JsonProperty("notifications")
    public final List<NotificationInfo> notifications;

    public NotificationsResponse(
        int count,
        List<NotificationInfo> notifications
    ) {
        this.count = count;
        this.notifications = notifications;
    }
}
