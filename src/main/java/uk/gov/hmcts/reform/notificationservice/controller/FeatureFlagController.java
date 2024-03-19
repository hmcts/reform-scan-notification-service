package uk.gov.hmcts.reform.notificationservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.notificationservice.launchdarkly.LaunchDarklyClient;

import static org.springframework.http.ResponseEntity.ok;

/**
 * The FeatureFlagController class in Java retrieves and returns the status of a specific feature flag using a
 * LaunchDarklyClient.
 */
@RestController
public class FeatureFlagController {
    private final LaunchDarklyClient featureToggleService;

    public FeatureFlagController(LaunchDarklyClient featureToggleService) {
        this.featureToggleService = featureToggleService;
    }

    /**
     * This function retrieves the status of a specific feature flag and returns it as a response entity.
     *
     * @param flag The `flag` parameter in the `flagStatus` method represents the feature flag that you want
     *             to check the status of. The method calls the `isFeatureEnabled` method from the
     *             `featureToggleService` to determine if the specified feature flag is enabled or not.
     * @return A ResponseEntity of type String object is being returned, which contains the flag name and its
     *      corresponding status (enabled or disabled).
     */
    @GetMapping("/feature-flags/{flag}")
    public ResponseEntity<String> flagStatus(@PathVariable String flag) {
        boolean isEnabled = featureToggleService.isFeatureEnabled(flag);
        return ok(flag + " : " + isEnabled);
    }
}
