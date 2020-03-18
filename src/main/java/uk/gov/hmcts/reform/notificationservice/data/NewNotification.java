package uk.gov.hmcts.reform.notificationservice.data;

import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

public class NewNotification {

    public final String zipFileName;
    public final String poBox;
    public final String service;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;

    public NewNotification(
        String zipFileName,
        String poBox,
        String service,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription
    ) {
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.service = service;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }
}
