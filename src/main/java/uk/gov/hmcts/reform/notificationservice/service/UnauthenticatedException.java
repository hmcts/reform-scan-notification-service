package uk.gov.hmcts.reform.notificationservice.service;

public class UnauthenticatedException extends RuntimeException {
    private static final long serialVersionUID = -4672282254380424023L;

    public UnauthenticatedException(String message) {
        super(message);
    }
}
