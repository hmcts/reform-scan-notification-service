package uk.gov.hmcts.reform.notificationservice.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

/**
 * The `AuthService` class in Java provides a method `authenticate` that validates an authentication token from an
 * authentication header and returns the service name.
 */
@Component
public class AuthService {

    private final AuthTokenValidator authTokenValidator;

    public AuthService(AuthTokenValidator authTokenValidator) {
        this.authTokenValidator = authTokenValidator;
    }

    /**
     * The function `authenticate` takes an authentication header as input and returns the service
     * name after validating the authentication token.
     *
     * @param authHeader The `authHeader` parameter is a string that typically contains authentication
     *                   information, such as a token or credentials, passed in the HTTP header of a request.
     * @return The method `authenticate` returns the service name extracted from the authentication
     *      header using the `authTokenValidator`.
     */
    public String authenticate(String authHeader) {
        if (authHeader == null) {
            throw new UnauthenticatedException("Missing ServiceAuthorization header");
        } else {
            return authTokenValidator.getServiceName(authHeader);
        }
    }
}
