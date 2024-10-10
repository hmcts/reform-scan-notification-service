package uk.gov.hmcts.reform.notificationservice.data;

public enum NotificationStatus {

    PENDING,
    FAILED,
    SENT,
    // To be used when there are issues and records needs to be closed manually
    MANUALLY_HANDLED,
    //Initial insertion status for notifications created by the POST endpoint
    CREATED
}
