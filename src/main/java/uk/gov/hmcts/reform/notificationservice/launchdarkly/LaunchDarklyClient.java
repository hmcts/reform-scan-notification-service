package uk.gov.hmcts.reform.notificationservice.launchdarkly;


import com.launchdarkly.sdk.LDContext;
import com.launchdarkly.sdk.server.interfaces.DataSourceStatusProvider;
import com.launchdarkly.sdk.server.interfaces.LDClientInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LaunchDarklyClient {

    public final LDContext reformScanNotificationContext;

    private final LDClientInterface internalClient;

    @Autowired
    public LaunchDarklyClient(
        LaunchDarklyClientFactory launchDarklyClientFactory,
        @Value("${launchdarkly.sdk-key:YYYYY}") String sdkKey,
        @Value("${launchdarkly.offline-mode:false}") Boolean offlineMode
    ) {
        this.internalClient = launchDarklyClientFactory.create(sdkKey, offlineMode);
        this.reformScanNotificationContext = LDContext.builder(sdkKey).build();
    }

    public boolean isFeatureEnabled(String feature) {
        return internalClient.boolVariation(feature, reformScanNotificationContext,
                                            false);
    }

    public boolean isFeatureEnabled(String feature, LDContext context) {
        return internalClient.boolVariation(feature, context, false);
    }

    public DataSourceStatusProvider.Status getDataSourceStatus() {
        return internalClient.getDataSourceStatusProvider().getStatus();
    }
}
