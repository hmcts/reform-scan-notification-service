package uk.gov.hmcts.reform.notificationservice.exception;

import java.io.Serial;

public class BadRequestException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 7579941826346533850L;

    public BadRequestException(Integer notificationId) {
        super("A BadRequestException was received from supplier. Notification has been marked as a fail. Notification ID: : " + notificationId);
    }

    public BadRequestException(Exception exception) {
        super(exception);
    }
}
