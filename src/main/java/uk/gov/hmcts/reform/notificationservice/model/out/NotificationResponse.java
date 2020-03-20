package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.joda.ser.InstantSerializer;

import java.time.Instant;

public class NotificationResponse {
    @JsonProperty("notification_id")
    public final String notificationId;

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("service")
    public final String service;

    @JsonProperty("document_control_number")
    public final String documentControlNumber;

    @JsonProperty("error_code")
    public final String errorCode;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("created_at")
    public final Instant createdAt;

    @JsonSerialize(using = InstantSerializer.class)
    @JsonProperty("processed_at")
    public final Instant processedAt;

    @JsonProperty("status")
    public final String status;

    public NotificationResponse(
        String notificationId,
        String zipFileName,
        String poBox,
        String service,
        String documentControlNumber,
        String errorCode,
        Instant createdAt,
        Instant processedAt,
        String status
    ) {
        this.notificationId = notificationId;
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.service = service;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.status = status;
    }
}
