package uk.gov.hmcts.reform.notificationservice.data.notifications;

import java.time.Instant;

public class Notification {

    public final long id;
    public final String zipFileName;
    public final String poBox;
    public final String documentControlNumber;
    public final String errorCode;
    public final String errorDescription;
    public final Instant createdAt;
    public final Instant processedAt;
    public final String service;
    public final Status status;

    public Notification(
        long id,
        String zipFileName,
        String poBox,
        String documentControlNumber,
        String errorCode,
        String errorDescription,
        Instant createdAt,
        Instant processedAt,
        String service,
        Status status
    ) {
        this.id = id;
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.service = service;
        this.status = status;
    }
}
