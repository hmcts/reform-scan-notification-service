package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class NotificationsResponse {

    @JsonProperty("count")
    @Schema(title = "Notification count", name = "count", description = "Number of notifications found")
    public final int count;

    @JsonProperty("notifications")
    @Schema(title = "List of notifications", name = "notifications", description = "Full list of notifications found")
    public final List<NotificationInfo> notifications;

    public NotificationsResponse(
        int count,
        List<NotificationInfo> notifications
    ) {
        this.count = count;
        this.notifications = notifications;
    }
}
