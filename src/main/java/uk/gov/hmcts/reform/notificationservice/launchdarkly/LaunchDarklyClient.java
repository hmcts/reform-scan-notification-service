package uk.gov.hmcts.reform.notificationservice.launchdarkly;


import com.launchdarkly.sdk.LDUser;
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    public boolean isFeatureEnabled(String feature) {
        return internalClient.boolVariation(feature, LaunchDarklyClient.REFORM_SCAN_NOTIFICATION_SERVICE_USER,
                                            false);
    }

    public boolean isFeatureEnabled(String feature, LDUser user) {
        return internalClient.boolVariation(feature, user, false);
    }

    public DataSourceStatusProvider.Status getDataSourceStatus() {
        return internalClient.getDataSourceStatusProvider().getStatus();
    }
}
