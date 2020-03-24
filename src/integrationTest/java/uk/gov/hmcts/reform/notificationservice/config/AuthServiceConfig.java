package uk.gov.hmcts.reform.notificationservice.config;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;

import java.util.List;

@Configuration
public class AuthServiceConfig {

    @Bean
    @ConditionalOnProperty(name = "s2s-auth.url", havingValue = "false")
    public AuthTokenValidator tokenValidator() {
        return new AuthTokenValidator() {
            @Override
            public void validate(String token) {
                throw new NotImplementedException();
            }

            @Override
            public void validate(String token, List<String> roles) {
                throw new NotImplementedException();
            }

            @Override
            public String getServiceName(String token) {
                return "some_service_name";
            }
        };
    }
}
