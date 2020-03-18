package uk.gov.hmcts.reform.notificationservice.data;

import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.time.Instant;

public class Notification {

    public final long id;
    public final String notificationId;
    public final String zipFileName;
    public final String poBox;
    public final String service;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;
    public final Instant createdAt;
    public final Instant processedAt;
    public final NotificationStatus status;

    public Notification(
        long id,
        String notificationId,
        String zipFileName,
        String poBox,
        String service,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        Instant createdAt,
        Instant processedAt,
        NotificationStatus status
    ) {
        this.id = id;
        this.notificationId = notificationId;
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.service = service;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.status = status;
    }
}
