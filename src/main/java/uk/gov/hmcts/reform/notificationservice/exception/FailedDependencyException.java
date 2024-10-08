package uk.gov.hmcts.reform.notificationservice.exception;

import feign.FeignException;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;

import java.io.Serial;

/**
 * Exception Class - Should be thrown when a dependency of the service fails. For example, it is to be used
 * when a request to the service is received to create a notification and the service fails in trying to make
 * a request to a supplier's external endpoint.
 */
public class FailedDependencyException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6579941826346533850L;

    private NotificationInfo notificationInfo;

    /**
     * Constructs a new failed dependency exception with the specified detail message.
     *
     * @param notificationInfo the info of the notification that was involved in the failed request
     * @param dependencyException the original exception that occurred when trying to make the request to the dependency
     */
    public FailedDependencyException(NotificationInfo notificationInfo, FeignException dependencyException) {
        super(String.format("The service's client failed to make a request to an external endpoint: Client received status code: %s. " +
                                "Client received response: %s.",
                            dependencyException.status(),
                            dependencyException.contentUTF8()));
        this.notificationInfo = notificationInfo;
    }

    public NotificationInfo getNotificationInfo() {
        return notificationInfo;
    }
}
