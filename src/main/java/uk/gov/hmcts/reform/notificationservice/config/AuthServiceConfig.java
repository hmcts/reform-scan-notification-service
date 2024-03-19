package uk.gov.hmcts.reform.notificationservice.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

@Configuration
public class AuthServiceConfig {

    /**
     * This function creates a bean for token validation based on a conditional property
     * and a service authorisation API.
     *
     * @param s2sApi The `s2sApi` parameter is likely an instance of the `ServiceAuthorisationApi` class,
     *               which is used for communicating with a service authorization API. It is being passed as a
     *               dependency to the `ServiceAuthTokenValidator` constructor in the `tokenValidator`
     *               bean definition.
     * @return An instance of `ServiceAuthTokenValidator` is being returned, which is constructed using the
     *      `s2sApi` bean as a parameter.
     */
    @Bean
    @ConditionalOnProperty(name = "idam.s2s-auth.url")
    public AuthTokenValidator tokenValidator(ServiceAuthorisationApi s2sApi) {
        return new ServiceAuthTokenValidator(s2sApi);
    }
}
