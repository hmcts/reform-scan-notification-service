package uk.gov.hmcts.reform.bulkscan.payment.processor;

import com.typesafe.config.ConfigFactory;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.equalTo;

@TestPropertySource("classpath:application.conf")
class NotificationServiceHealthTest {

    private static final String TEST_URL = ConfigFactory.load().getString("test-url");

    @Test
    void notification_service_is_healthy() {
        // TODO: this test is there so that a test report can be created for smoke tests
        // (otherwise the build fails). Remove when actual functional tests have been written.
        RestAssured
            .given()
            .relaxedHTTPSValidation()
            .baseUri(TEST_URL)
            .get("/health")
            .then()
            .assertThat()
            .statusCode(HttpStatus.OK.value())
            .and()
            .body("status", equalTo(Status.UP.toString()));
    }
}
