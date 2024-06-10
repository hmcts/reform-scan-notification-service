package uk.gov.hmcts.reform.notificationservice.clients;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@FeignClient(
    name = "error-notifications-secondary",
    url = "${clients.error-notifications.url.secondary}",
    configuration = ErrorNotificationConfiguration.class
)
public interface ErrorNotificationClientSecondary {

    @PostMapping(value = "/notifications",
        consumes = APPLICATION_JSON_VALUE,
        produces = APPLICATION_JSON_VALUE
    )
    ErrorNotificationResponse notify(@RequestBody ErrorNotificationRequest notification);
}
