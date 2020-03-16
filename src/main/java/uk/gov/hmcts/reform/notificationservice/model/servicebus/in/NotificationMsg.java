package uk.gov.hmcts.reform.notificationservice.model.servicebus.in;

import com.fasterxml.jackson.annotation.JsonProperty;

public class NotificationMsg {

    public final String zipFileName;
    public final String jurisdiction;
    public final String poBox;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;
    public final String service;

    @SuppressWarnings("squid:S00107") // number of params
    public NotificationMsg(
        @JsonProperty(value = "zipFileName", required = true) String zipFileName,
        @JsonProperty("jurisdiction") String jurisdiction,
        @JsonProperty("poBox") String poBox,
        @JsonProperty("documentControlNumber") String documentControlNumber,
        @JsonProperty(value = "errorCode", required = true) ErrorCode errorCode,
        @JsonProperty(value = "errorDescription", required = true) String errorDescription,
        @JsonProperty(value = "service", required = true) String service
    ) {
        this.zipFileName = zipFileName;
        this.jurisdiction = jurisdiction;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
    }

}

