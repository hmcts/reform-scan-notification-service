package uk.gov.hmcts.reform.notificationservice.exception;

public class DuplicateMessageIdException extends RuntimeException {

    public DuplicateMessageIdException(String message) {
        super(message);
    }
}
