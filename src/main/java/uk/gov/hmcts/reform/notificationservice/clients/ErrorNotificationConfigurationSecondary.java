package uk.gov.hmcts.reform.notificationservice.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

public class ErrorNotificationConfigurationSecondary {

    @Bean
    Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor(
        @Value("${clients.error-notifications.secondary.username}") String username,
        @Value("${clients.error-notifications.secondary.password}") String password
    ) {
        return new BasicAuthRequestInterceptor(username, password, StandardCharsets.UTF_8);
    }
}
