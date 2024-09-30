package uk.gov.hmcts.reform.notificationservice.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.List;

@ControllerAdvice
@Slf4j
public class GlobalControllerExceptionHandler {

    private static final String MESSAGE = "message";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Handles NotFoundException.
     *
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
        return new ResponseEntity<>(new ObjectMapper().writeValueAsString(error), responseHeaders, HttpStatus.NOT_FOUND);
    }
}
