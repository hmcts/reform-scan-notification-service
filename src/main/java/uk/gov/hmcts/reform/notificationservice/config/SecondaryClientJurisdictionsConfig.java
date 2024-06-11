package uk.gov.hmcts.reform.notificationservice.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class SecondaryClientJurisdictionsConfig {

    private static final Logger log = LoggerFactory.getLogger(SecondaryClientJurisdictionsConfig.class);

    // Set it to be empty if an env var is not set
    @Value("${clients.error-notifications.secondary.jurisdictions:}")
    private String jurisdictions;

    public String[] getJurisdictionList() {
        log.debug("Secondary clients jurisdictions initial: {}", jurisdictions);
        String[] secondaryClientJurisdictionList =
            Arrays.stream(jurisdictions.split(",")).map(String::toLowerCase).toArray(String[]::new);
        log.debug("Secondary clients jurisdictions constructed: {}",
                 Arrays.toString(secondaryClientJurisdictionList));
        return secondaryClientJurisdictionList;
    }
}
