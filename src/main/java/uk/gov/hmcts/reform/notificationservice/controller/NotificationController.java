package uk.gov.hmcts.reform.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;

@RestController
@RequestMapping(path = "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Information on notifications", description = "Endpoint for notifications present for this service")
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

    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Success",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthenticated / Invalid token"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error validating service header"
        )
    })
    @GetMapping
    @Operation(
        method = "GET",
        summary = "Get list of error notifications",
        description = "Get list of error notifications for specific file name and service",
        parameters = @Parameter(
            in = ParameterIn.QUERY,
            name = "file_name",
            description = "File name to look up",
            example = "2000000000000_24-06-2020-12-28-19.example.zip"
        )
    )
    public NotificationsResponse getNotifications(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestParam("file_name") String fileName
    ) {
        String serviceName = authService.authenticate(serviceAuthHeader);
        return mapToNotificationsResponse(notificationService.findByFileNameAndService(fileName, serviceName));
    }

    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Success",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationsResponse.class)
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Error getting data"
        )
    })
    @GetMapping(params = "date")
    @Operation(
        method = "GET",
        summary = "Get list of error notifications",
        description = "Get list of error notifications for specific date",
        parameters = @Parameter(
            in = ParameterIn.QUERY,
            name = "date",
            description = "Date boundary to look-up notifications in",
            example = "2020-06-24"
        )
    )
    public NotificationsResponse getNotificationsByDate(
        @RequestParam(name = "date") @DateTimeFormat(iso = DATE) LocalDate date
    ) {
        return mapToNotificationsResponse(notificationService.findByDate(date));
    }

    private NotificationsResponse mapToNotificationsResponse(List<Notification> list) {
        List<NotificationInfo> notifications = list.stream()
            .map(notification -> toNotificationResponse(notification))
            .collect(toList());
        return new NotificationsResponse(notifications.size(), notifications);
    }

    private NotificationInfo toNotificationResponse(Notification notification) {
        return new NotificationInfo(
            notification.id,
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
