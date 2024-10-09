package uk.gov.hmcts.reform.notificationservice.model.in;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

/**
 * A representation of an external request to the micrososervice to notify suppliers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class NotifyRequest {

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

}
