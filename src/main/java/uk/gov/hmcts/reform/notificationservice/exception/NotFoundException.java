package uk.gov.hmcts.reform.notificationservice.exception;

import java.io.Serial;

public class NotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6579941826346533850L;

    /**
     * Constructs a new not found exception with the specified detail message.
     *
     * @param message the detail message
     */
    public NotFoundException(String message) {
        super("Not found: " + message);
    }

    /**
     * Constructs a new not found exception with the specified detail message and inner exception.
     *
     * @param exception the inner exception
     */
    public NotFoundException(Exception exception) {
        super(exception);
    }
}
