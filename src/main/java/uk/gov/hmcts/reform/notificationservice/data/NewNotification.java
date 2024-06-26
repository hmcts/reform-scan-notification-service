package uk.gov.hmcts.reform.notificationservice.data;

import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

public class NewNotification {

    public final String zipFileName;
    public final String poBox;
    public final String container;
    public final String service;
    public final String documentControlNumber;
    public final ErrorCode errorCode;
    public final String errorDescription;
    public final String messageId;
    public final String client;

    @SuppressWarnings("squid:S00107") // number of params
    public NewNotification(
        String zipFileName,
        String poBox,
        String container,
        String service,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        String messageId,
        String client
    ) {
        this.zipFileName = zipFileName;
        this.poBox = poBox;
        this.service = service;
        this.container = container;
        this.documentControlNumber = documentControlNumber;
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
        this.messageId = messageId;
        this.client = client;
    }
}
