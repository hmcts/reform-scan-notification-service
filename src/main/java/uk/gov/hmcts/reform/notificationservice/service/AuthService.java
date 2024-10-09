package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

@Component
public class AuthService {

    private final AuthTokenValidator authTokenValidator;

    public AuthService(AuthTokenValidator authTokenValidator) {
        this.authTokenValidator = authTokenValidator;
    }

    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }

    public void validate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            authTokenValidator.validate(authHeader);
        }
    }
}
