package uk.gov.hmcts.reform.notificationservice.config;

//TODO: FACT-2026 - whole class can go
public class PendingMigrationException extends RuntimeException {

    private static final long serialVersionUID = -3892580157302271206L;

    public PendingMigrationException(String message) {
        super(message);
    }
}
