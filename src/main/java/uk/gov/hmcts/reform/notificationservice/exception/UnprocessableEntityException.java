package uk.gov.hmcts.reform.notificationservice.exception;

import java.io.Serial;
import java.util.List;

public class UnprocessableEntityException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8579941826346533850L;

    public UnprocessableEntityException(List<String> fields) {
        super("These fields of the notification message did not pass validation: " + String.join(" ", fields));
    }

    public UnprocessableEntityException(String notificationId) {
        super("Supplier was unable to process the notification request. Notification ID: ");
    }

    public UnprocessableEntityException(Exception exception) {
        super(exception);
    }
}
