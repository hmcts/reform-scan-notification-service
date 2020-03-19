package uk.gov.hmcts.reform.notificationservice.exception;

public class UnknownMessageProcessingResultException extends RuntimeException {

    public UnknownMessageProcessingResultException(String message) {
        super(message);
    }
}
