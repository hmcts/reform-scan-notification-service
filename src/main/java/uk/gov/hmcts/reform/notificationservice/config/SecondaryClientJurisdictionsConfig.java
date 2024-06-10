package uk.gov.hmcts.reform.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.authorisation.ServiceAuthorisationApi;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.authorisation.validators.ServiceAuthTokenValidator;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SecondaryClientJurisdictionsConfig {

    private static final Logger log = LoggerFactory.getLogger(SecondaryClientJurisdictionsConfig.class);

    @Value("${clients.error_notifications_secondary.jurisdictions}")
    private String jurisdictions;

    public String[] getJurisdictionList() {
        // TODO: switch to debug
        log.info("Secondary clients jurisdictions initial: {}", jurisdictions);
        String[] secondaryClientJurisdictionList =
            Arrays.stream(jurisdictions.split(",")).map(String::toLowerCase).toArray(String[]::new);
        log.info("Secondary clients jurisdictions constructed: {}",
                 Arrays.toString(secondaryClientJurisdictionList));
        return secondaryClientJurisdictionList;
    }
}
