package uk.gov.hmcts.reform.notificationservice.exception;

//TODO: FACT-2026 - whole class can go
public class UnknownMessageProcessingResultException extends RuntimeException {

    public UnknownMessageProcessingResultException(String message) {
        super(message);
    }
}
