package uk.gov.hmcts.reform.notificationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import static org.mockito.Mockito.mock;

@Configuration
public class AuthServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url", havingValue = "false")
    public AuthTokenValidator tokenValidator() {
        return mock(AuthTokenValidator.class);
    }
}
