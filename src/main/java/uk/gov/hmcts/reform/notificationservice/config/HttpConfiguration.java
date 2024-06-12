package uk.gov.hmcts.reform.notificationservice.config;

import feign.Client;
import feign.httpclient.ApacheHttpClient;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * The `HttpConfiguration` class in Java configures and provides instances of Apache HttpClient and RestTemplate with
 * specific timeout settings.
 */
@Configuration
public class HttpConfiguration {

    @Bean
    public Client getFeignHttpClient() {
        return new ApacheHttpClient(getHttpClient());
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    /**
     * The function creates and returns an instance of HttpComponentsClientHttpRequestFactory using a custom HttpClient.
     *
     * @return An instance of `HttpComponentsClientHttpRequestFactory` initialized with a `Http5Client` instance.
     */
    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        return new HttpComponentsClientHttpRequestFactory(getHttp5Client());
    }

    /**
     * The function returns an Apache HttpClient instance configured with specific connection request timeout settings.
     *
     * @return An instance of the Apache HttpClient version 5 is being returned.
     */
    private org.apache.hc.client5.http.classic.HttpClient getHttp5Client() {
        org.apache.hc.client5.http.config.RequestConfig config =
            org.apache.hc.client5.http.config.RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                .build();

        return org.apache.hc.client5.http.impl.classic.HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }

    /**
     * The function returns an instance of HttpClient configured with specific timeout settings.
     *
     * @return An instance of HttpClient with the specified configuration settings is being returned.
     */
    private HttpClient getHttpClient() {
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(30000)
            .setConnectionRequestTimeout(30000)
            .setSocketTimeout(60000)
            .build();

        return HttpClientBuilder
            .create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();
    }
}
