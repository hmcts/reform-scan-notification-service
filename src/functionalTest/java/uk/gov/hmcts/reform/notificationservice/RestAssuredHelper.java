package uk.gov.hmcts.reform.notificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.logging.appinsights.SyntheticHeaders;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.notificationservice.Configuration.TEST_S2S_SECRET;
import static uk.gov.hmcts.reform.notificationservice.Configuration.TEST_S2S_URL;
import static uk.gov.hmcts.reform.notificationservice.Configuration.TEST_URL;

final class RestAssuredHelper {

    private static final String SERVICE_AUTH_HEADER = "ServiceAuthorization";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RestAssuredHelper() {
        // utility class construct
    }

    static String s2sSignIn(String microservice) {
        Map<String, Object> params = ImmutableMap.of(
            "microservice", microservice,
            "oneTimePassword", new GoogleAuthenticator().getTotpPassword(TEST_S2S_SECRET)
        );

        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_S2S_URL)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .body(params)
            .when()
            .post("/lease")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        return response
            .getBody()
            .asString();
    }

    static JsonNode getNotification(String s2sToken, String zipFilename) {
        Response response = RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_URL)
            .header(SERVICE_AUTH_HEADER, "Bearer " + s2sToken)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header(SyntheticHeaders.SYNTHETIC_TEST_SOURCE, "Reform Scan Notification Service functional test")
            .queryParam("file_name", zipFilename)
            .when()
            .get("/notifications")
            .andReturn();

        assertThat(response.getStatusCode()).isEqualTo(200);

        try {
            return MAPPER.readTree(response.getBody().asByteArray());
        } catch (IOException exception) {
            throw new RuntimeException(
                "Unable to read body from notification endpoint. Body: " + response.getBody().prettyPrint()
            );
        }
    }
}
