package uk.gov.hmcts.reform.notificationservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;
import uk.gov.hmcts.reform.notificationservice.service.UnauthenticatedException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.http.ResponseEntity.status;

@OpenAPIDefinition(
    info = @Info(
        title = "Error Notification Service",
        version = "1",
        description = "API consuming Azure ServiceBus messages and publishing error messages to the scan supplier",
        license = @License(name = "MIT", url = "https://opensource.org/licenses/MIT")
    )
)
@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    private static final String MESSAGE = "message";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Handles NotFoundException.
     * Puts the exception message into the response body and returns a status code of 404 (NOT FOUND)
     * @param ex the exception
     * @return the response entity
     * @throws JsonProcessingException if the response entity cannot be converted to a string
     */
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<String> notFoundExceptionHandler(final NotFoundException ex) throws JsonProcessingException {
        HashMap<String, String> error = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(CONTENT_TYPE, APPLICATION_JSON);
        error.put(MESSAGE, ex.getMessage());
        return new ResponseEntity<>(new ObjectMapper().writeValueAsString(error),
                                    responseHeaders, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles the NumberFormat exception.
     * Puts the malformed number and a message about the number being invalid into the response body with a status code
     * of 400 (BAD REQUEST)
     * @param ex the exception
     * @return the response entity
     * @throws JsonProcessingException if the response entity cannot be converted to a string
     */
    @ExceptionHandler(NumberFormatException.class)
    ResponseEntity<String> notFoundExceptionHandler(final NumberFormatException ex) throws JsonProcessingException {
        HashMap<String, String> error = new HashMap<>();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(CONTENT_TYPE, APPLICATION_JSON);
        error.put("Info", ex.getMessage());
        error.put(MESSAGE, "Invalid number. You must use a whole number e.g. not decimals like 13.0 and not letters");
        return new ResponseEntity<>(new ObjectMapper().writeValueAsString(error),
                                    responseHeaders, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles the FailedDependency exception.
     * Puts the notification info associated with the exception into the response body and returns a status code of
     * 424 (FAILED DEPENDENCY)
     * @param ex the exception
     * @return ResponseEntity with 424 status code
     * @throws JsonProcessingException if the response entity cannot be converted to a string
     */
    @ExceptionHandler(FailedDependencyException.class)
    ResponseEntity<NotificationInfo> failedDependencyExceptionHandler(final FailedDependencyException ex)
        throws JsonProcessingException {
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(CONTENT_TYPE, APPLICATION_JSON);
        log.error(ex.getMessage(), ex);
        return new ResponseEntity<>(ex.getNotificationInfo(), responseHeaders, HttpStatus.FAILED_DEPENDENCY);
    }

    /**
     * Handles the MethodArgumentNotValid exception.
     * When the body of the request has correct syntax but the values fail validation the
     * MethodArgumentNotValidException is thrown. This method takes the fields that failed validation and the
     * reasons why and puts them in the response body with a status code of 400 (BAD REQUEST)
     * @param ex the exception
     * @return ResponseEntity with 400 status code
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handle(MethodArgumentNotValidException ex) {
        Map<String, String> errorMap = new ConcurrentHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errorMap.put(error.getField(),
                                                                             error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
    }

    /**
     * Handles the UnauthenticatedException.
     * UnauthenticatedException is thrown when authentication is not present, This method handles
     * the exception by returning a status code of 401 (UNAUTHORIZED)
     * @param ex the exception
     * @return ResponseEntity with 401 status code
     */
    @ExceptionHandler(UnauthenticatedException.class)
    protected ResponseEntity<Void> handleUnauthenticatedException(UnauthenticatedException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Handles the InvalidTokenException.
     * When the authentication library determines that a token is not valid the InvalidTokenException is thrown.
     * This method handles the exception by returning a status code of 401 (UNAUTHORIZED).
     * @param ex the exception
     * @return ResponseEntity with 401 status code
     */
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<Void> handleInvalidTokenException(InvalidTokenException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Handles the ServiceException.
     * When the authentication library encounters a non-Feign exception trying to authenticate a token the
     * ServiceException is thrown. This method handles the exception by returning a status code of
     * 500 (INTERNAL_SERVER_ERROR).
     * @param ex the exception
     * @return ResponseEntity with 401 status code
     */
    @ExceptionHandler(ServiceException.class)
    protected ResponseEntity<Void> handleServiceException(ServiceException ex) {
        log.error(ex.getMessage(), ex);
        return status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
