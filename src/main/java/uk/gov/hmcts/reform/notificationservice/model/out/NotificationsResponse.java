package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public NotificationsResponse(List<NotificationInfo> notifications) {
        this.notifications = notifications.stream()
            .sorted(Comparator.comparingLong(i -> Long.parseLong(i.id)))
            .collect(Collectors.toList());
        this.count = setNotificationsCount();
        this.sentNotificationsCount = setSentNotificationsCount();
        this.pendingNotificationsCount = setPendingNotificationsCount();
    }

    private int setNotificationsCount() {
        return notifications.size();
    }

    private int setSentNotificationsCount() {
        return notifications.stream()
            .filter(n -> n.status.equals(NotificationStatus.SENT.name()))
            .collect(Collectors.toList()).size();
    }

    private int setPendingNotificationsCount() {
        return notifications.stream()
            .filter(n -> n.status.equals(NotificationStatus.PENDING.name()))
            .collect(Collectors.toList()).size();
    }
}
