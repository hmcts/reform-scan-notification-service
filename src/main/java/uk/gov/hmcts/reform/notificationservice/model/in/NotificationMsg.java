package uk.gov.hmcts.reform.notificationservice.model.in;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

//TODO: FACT-2026 - whole class can go
public class NotificationMsg {

    public final String zipFileName;
    public final String jurisdiction;
    public final String poBox;
    public final String container;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;
    public final String service;

    @SuppressWarnings("squid:S00107") // number of params
    public NotificationMsg(
        @JsonProperty(value = "zip_file_name", required = true) String zipFileName,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonProperty("po_box") String poBox,
        @JsonProperty("container") String container,
        @JsonProperty("document_control_number") String documentControlNumber,
        @JsonProperty(value = "error_code", required = true) ErrorCode errorCode,
        @JsonProperty(value = "error_description", required = true) String errorDescription,
        @JsonProperty(value = "service", required = true) String service
    ) {
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.container = container;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
    }

}

