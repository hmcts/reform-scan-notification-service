package uk.gov.hmcts.reform.notificationservice.launchdarkly;

import com.launchdarkly.sdk.server.LDClient;
import com.launchdarkly.sdk.server.LDConfig;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.stereotype.Service;

/**
 * The `LaunchDarklyClientFactory` class in Java creates and returns a
 * new `LDClientInterface` object with a provided SDK key and offline mode setting.
 */
@Service
public class LaunchDarklyClientFactory {

    /**
     * The function creates and returns a new LDClientInterface object with the provided
     * SDK key and offline mode setting.
     *
     * @param sdkKey The `sdkKey` parameter is a unique key that identifies your LaunchDarkly environment.
     *               It is used to authenticate your application with LaunchDarkly and retrieve feature
     *               flag configurations.
     * @param offlineMode The `offlineMode` parameter is a boolean flag that indicates whether the client
     *                    should operate in offline mode. When `offlineMode` is set to `true`, the client
     *                    will not make network requests to the LaunchDarkly servers and will instead use
     *                    cached data or default values.
     * @return An instance of LDClientInterface is being returned.
     */
    public LDClientInterface create(String sdkKey, boolean offlineMode) {
        LDConfig config = new LDConfig.Builder()
            .offline(offlineMode)
            .build();
        return new LDClient(sdkKey, config);
    }
}
