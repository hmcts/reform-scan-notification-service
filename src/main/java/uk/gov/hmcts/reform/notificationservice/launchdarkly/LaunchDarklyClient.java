package uk.gov.hmcts.reform.notificationservice.launchdarkly;


import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The `LaunchDarklyClient` class in Java provides methods to check feature enablement status
 * for users and retrieve data source status using LaunchDarkly's internal client.
 */
@Service
public class LaunchDarklyClient {
    public static final LDUser REFORM_SCAN_NOTIFICATION_SERVICE_USER = new LDUser.Builder("reform-scan-notification"
                                                                                              + "-service")
        .anonymous(true)
        .build();

    private final LDClientInterface internalClient;

    @Autowired
    public LaunchDarklyClient(
        LaunchDarklyClientFactory launchDarklyClientFactory,
        @Value("${launchdarkly.sdk-key:YYYYY}") String sdkKey,
        @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode
    ) {
        this.internalClient = launchDarklyClientFactory.create(sdkKey, offlineMode);
    }

    /**
     * The function `isFeatureEnabled` returns a boolean value indicating whether a feature is enabled
     * for a specific user.
     *
     * @param feature The `feature` parameter is a string that represents the name of the feature
     *                whose enablement status you want to check.
     * @return A boolean value is being returned, indicating whether the specified feature is enabled or not.
     */
    public boolean isFeatureEnabled(String feature) {
        return internalClient.boolVariation(feature, LaunchDarklyClient.REFORM_SCAN_NOTIFICATION_SERVICE_USER,
                                            false);
    }

    /**
     * The function `isFeatureEnabled` checks if a feature is enabled for a given user using LaunchDarkly's internal
     * client.
     *
     * @param feature The `feature` parameter is a string that represents the name of the feature for which
     *                you want to check if it is enabled for a given user.
     * @param user The `user` parameter is an object of type `LDUser`, which represents a user for whom you
     *             want to check if a feature is enabled.
     * @return The method `isFeatureEnabled` returns a boolean value indicating whether the specified feature is enabled
     *      for the given user.
     */
    public boolean isFeatureEnabled(String feature, LDUser user) {
        return internalClient.boolVariation(feature, user, false);
    }

    /**
     * The function returns the status of a data source using an internal client.
     *
     * @return The method `getDataSourceStatus()` is returning the status of the data source, which is of type
     *      `DataSourceStatusProvider.Status`.
     */
    public DataSourceStatusProvider.Status getDataSourceStatus() {
        return internalClient.getDataSourceStatusProvider().getStatus();
    }
}
