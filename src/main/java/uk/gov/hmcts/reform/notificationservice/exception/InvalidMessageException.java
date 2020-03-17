package uk.gov.hmcts.reform.notificationservice.exception;

import java.io.IOException;

public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(IOException exc) {}
}
