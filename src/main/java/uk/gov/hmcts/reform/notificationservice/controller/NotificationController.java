package uk.gov.hmcts.reform.notificationservice.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationsResponse;
import uk.gov.hmcts.reform.notificationservice.service.AuthService;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import java.util.List;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping(path = "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
public class NotificationController {
    private final NotificationService notificationService;
    private final AuthService authService;

    public NotificationController(
        NotificationService notificationService,
        AuthService authService
    ) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @GetMapping
    public NotificationsResponse getNotifications(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam("file_name") String fileName
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);

        List<NotificationInfo> notifications =
            notificationService.findByFileNameAndService(fileName, serviceName)
                .stream()
                .map(notification -> toNotificationResponse(notification))
                .collect(toList());

        return new NotificationsResponse(notifications.size(), notifications);
    }

    private NotificationInfo toNotificationResponse(Notification notification) {
        return new NotificationInfo(
            notification.confirmationId,
            notification.zipFileName,
            notification.poBox,
            notification.container,
            notification.service,
            notification.documentControlNumber,
            notification.errorCode.name(),
            notification.createdAt,
            notification.processedAt,
            notification.status.name()
        );
    }
}
