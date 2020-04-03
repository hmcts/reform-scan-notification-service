package uk.gov.hmcts.reform.notificationservice.model.out;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import uk.gov.hmcts.reform.notificationservice.util.CustomInstantSerializer;

import java.time.Instant;

public class NotificationInfo {
    @JsonProperty("confirmation_id")
    public final String confirmationId;

    @JsonProperty("zip_file_name")
    public final String zipFileName;

    @JsonProperty("po_box")
    public final String poBox;

    @JsonProperty("container")
    public final String container;

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
        String confirmationId,
        String zipFileName,
        String poBox,
        String container,
        String service,
        String documentControlNumber,
        String errorCode,
        Instant createdAt,
        Instant processedAt,
        String status
    ) {
        this.confirmationId = confirmationId;
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.container = container;
        this.service = service;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.createdAt = createdAt;
        this.processedAt = processedAt;
        this.status = status;
    }
}
