package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class NotificationsResponse {

    @JsonProperty("count")
    @Schema(title = "Notification count", name = "count", description = "Number of notifications found")
    public final int count;

    @JsonProperty("pendingNotificationsCount")
    @Schema(
        title = "Pending Notifications count",
        name = "pendingNotificationsCount",
        description = "Number of notifications with 'Pending' status"
    )
    public final int pendingNotificationsCount;

    @JsonProperty("sentNotificationsCount")
    @Schema(
        title = "Sent Notification count",
        name = "sentNotificationsCount",
        description = "Number of notifications with 'Sent' status"
    )
    public final int sentNotificationsCount;

    @JsonProperty("notifications")
    @Schema(title = "List of notifications", name = "notifications", description = "Full list of notifications found")
    public final List<NotificationInfo> notifications;

    public NotificationsResponse(
        int count,
        int sentNotificationsCount,
        int pendingNotificationsCount,
        List<NotificationInfo> notifications
    ) {
        this.count = count;
        this.notifications = notifications;
        this.sentNotificationsCount = sentNotificationsCount;
        this.pendingNotificationsCount = pendingNotificationsCount;
    }
}
