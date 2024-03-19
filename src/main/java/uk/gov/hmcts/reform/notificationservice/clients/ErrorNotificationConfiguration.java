package uk.gov.hmcts.reform.notificationservice.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

/**
 * The `ErrorNotificationConfiguration` class in Java defines beans for decoding and creating a
 * BasicAuthRequestInterceptor for error notifications.
 */
public class ErrorNotificationConfiguration {

    @Bean
    Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    /**
     * The function creates a BasicAuthRequestInterceptor bean with the provided username, password, and charset
     * for error notifications.
     *
     * @param username The username parameter is typically a string value that represents the username used
     *                 for basic authentication when making HTTP requests to a server or API. It is often
     *                 required for authenticating and authorizing access to certain resources.
     * @param password The password parameter in the code snippet is used to retrieve the value of
     *                 the "clients.error-notifications.password" property from the application configuration.
     *                 This password is likely used for basic authentication when making requests to a service
     *                 that requires authentication.
     * @return A BasicAuthRequestInterceptor bean is being returned.
     */
    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(
        @Value("${clients.error-notifications.username}") String username,
        @Value("${clients.error-notifications.password}") String password
    ) {
        return new BasicAuthRequestInterceptor(username, password, StandardCharsets.UTF_8);
    }
}
