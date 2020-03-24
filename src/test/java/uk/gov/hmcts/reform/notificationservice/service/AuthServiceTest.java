package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    private static final String SERVICE_HEADER = "some-header";

    @Mock
    private AuthTokenValidator validator;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(validator);
    }

    @AfterEach
    void tearDown() {
        reset(validator);
    }

    @Test
    void should_throw_missing_header_exception_when_it_is_null() {
        // when
        Throwable exception = catchThrowable(() -> service.authenticate(null));

        // then
        assertThat(exception)
            .isInstanceOf(UnauthenticatedException.class)
            .hasMessage("Missing ServiceAuthorization header");

        // and
        verify(validator, never()).getServiceName(anyString());
    }

    @Test
    void should_throw_when_invalid_token_received() {
        // given
        final String invalidToken = "invalid token";

        willThrow(new InvalidTokenException(invalidToken)).given(validator).getServiceName(anyString());

        // when
        InvalidTokenException exception = catchThrowableOfType(
            () -> service.authenticate(SERVICE_HEADER),
            InvalidTokenException.class
        );

        // then
        assertThat(exception.getMessage()).isEqualTo(invalidToken);
    }

    @Test
    void should_return_service_name_when_valid_token_received() {
        // given
        final String someServiceName = "some-service";

        given(validator.getServiceName(SERVICE_HEADER)).willReturn(someServiceName);

        // when
        String serviceName = service.authenticate(SERVICE_HEADER);

        // then
        assertThat(serviceName).isEqualTo(serviceName);
    }
}
