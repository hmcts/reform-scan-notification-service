package uk.gov.hmcts.reform.notificationservice.data.notifications;

public class NewNotification {

    final String zipFileName;
    final String poBox;
    final String documentControlNumber;
    final String errorCode;
    final String errorDescription;
    final String service;

    public NewNotification(
        String zipFileName,
        String poBox,
        String documentControlNumber,
        String errorCode,
        String errorDescription,
        String service
    ) {
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.service = service;
    }
}
