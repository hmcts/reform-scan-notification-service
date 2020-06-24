package uk.gov.hmcts.reform.notificationservice.exceptionhandler;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.notificationservice.service.UnauthenticatedException;

import static org.springframework.http.ResponseEntity.status;

// exception handler also plays part in specs generation so including basic info here
// to not overcrowd controller
@OpenAPIDefinition(
    info = @Info(
        title = "Error Notification Service",
        version = "1",
        description = "API consuming Azure ServiceBus messages and publishing error messages to the scan supplier",
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    )
)
@ControllerAdvice
public class ResponseExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ResponseExceptionHandler.class);

    @ExceptionHandler(UnauthenticatedException.class)
    protected ResponseEntity<Void> handleUnauthenticatedException(UnauthenticatedException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<Void> handleInvalidTokenException(InvalidTokenException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    @ExceptionHandler(ServiceException.class)
    protected ResponseEntity<Void> handleServiceException(ServiceException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
