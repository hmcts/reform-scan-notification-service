package uk.gov.hmcts.reform.notificationservice.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NotificationMsgRequest {

    @NotBlank
    @JsonProperty(value = "zip_file_name", required = true)
    public final String zipFileName;
    @JsonProperty("jurisdiction")
    public final String jurisdiction;
    @JsonProperty("po_box")
    public final String poBox;
    @NotBlank
    @JsonProperty("container")
    public final String container;
    @JsonProperty("document_control_number")
    public final String documentControlNumber;
    @NotNull(message = "An error code must be provided")
    @JsonProperty(value = "error_code", required = true)
    public final ErrorCode errorCode; //TODO: Once this can be turned into a String then better validation can be done
    @NotBlank
    @JsonProperty(value = "error_description", required = true)
    public final String errorDescription;
    @NotBlank
    @JsonProperty(value = "service", required = true)
    public final String service;

//    @SuppressWarnings("squid:S00107") // number of params
//    public NotificationMsg(
//       String zipFileName,
//        @JsonProperty("jurisdiction") String jurisdiction,
//        @JsonProperty("po_box") String poBox,
//        @JsonProperty("container") String container,
//        @JsonProperty("document_control_number") String documentControlNumber,
//        @JsonProperty(value = "error_code", required = true) ErrorCode errorCode,
//        @JsonProperty(value = "error_description", required = true) String errorDescription,
//        @JsonProperty(value = "service", required = true) String service
//    ) {
//        this.zipFileName = zipFileName;
//        this.jurisdiction = jurisdiction;
//        this.poBox = poBox;
//        this.container = container;
//        this.documentControlNumber = documentControlNumber;
//        this.errorCode = errorCode;
//        this.errorDescription = errorDescription;
//        this.service = service;
//    }

}
