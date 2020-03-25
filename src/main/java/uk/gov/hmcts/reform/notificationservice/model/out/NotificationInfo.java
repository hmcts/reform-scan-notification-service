package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.notificationservice.util.CustomInstantSerializer;

import java.time.Instant;

public class NotificationInfo {
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

    @JsonSerialize(using = CustomInstantSerializer.class)
    @JsonProperty("created_at")
    public final Instant createdAt;

    @JsonSerialize(using = CustomInstantSerializer.class)
    @JsonProperty("processed_at")
    public final Instant processedAt;

    @JsonProperty("status")
    public final String status;

    public NotificationInfo(
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
