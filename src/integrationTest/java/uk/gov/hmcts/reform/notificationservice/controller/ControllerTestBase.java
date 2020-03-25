package uk.gov.hmcts.reform.notificationservice.controller;

import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.authorisation.validators.AuthTokenValidator;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

public class ControllerTestBase {
    @MockBean protected NotificationService notificationService;
    @MockBean protected AuthTokenValidator tokenValidator;
}
