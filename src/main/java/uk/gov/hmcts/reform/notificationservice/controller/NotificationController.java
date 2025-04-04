package uk.gov.hmcts.reform.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.model.in.NotifyRequest;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationsResponse;
import uk.gov.hmcts.reform.notificationservice.service.AuthService;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.springframework.format.annotation.DateTimeFormat.ISO.DATE;
import static org.springframework.http.ResponseEntity.created;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping(path = "/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Information on notifications", description = "Endpoint for notifications present for this service")
public class NotificationController {
    private static final String BAD_REQUEST_CODE = "400";
    private static final String FAILED_DEPENDENCY_CODE = "424";
    private final NotificationService notificationService;
    private final AuthService authService;

    private static final int MAX_ERROR_DESCRIPTION_LENGTH = 1024;
    private static final String SUCCESS_CODE = "200";
    private static final String CREATED_CODE = "201";
    private static final String NOT_FOUND_CODE = "404";

    private static final String UNAUTHORISED_CODE = "401";

    private static final String UNAUTHORISED_USER = "Unauthorised user/Invalid token";


    public NotificationController(
        NotificationService notificationService,
        AuthService authService
    ) {
        this.notificationService = notificationService;
        this.authService = authService;
    }

    @ApiResponses(value =
        {
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
        }
    )
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

    @ApiResponses(value =
        {
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
        }
    )
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

    @ApiResponses(value =
        {
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
        }
    )
    @GetMapping(params = "zip_file_name")
    @Operation(
        method = "GET",
        summary = "Get list of error notifications",
        description = "Get list of error notifications by file name",
        parameters = @Parameter(
            in = ParameterIn.QUERY,
            name = "zip_file_name",
            description = "File name to look-up notifications",
            example = "2022304020414_17-08-2020-11-19-12.zip"
        )
    )
    public NotificationsResponse getNotificationsByZipFileName(
        @RequestParam(name = "zip_file_name") String zipFileName
    ) {
        return mapToNotificationsResponse(notificationService.findByZipFileName(zipFileName));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get a Notification by its ID")
    @ApiResponse(responseCode = SUCCESS_CODE, description = "Successful - Notification Found")
    @ApiResponse(responseCode = NOT_FOUND_CODE, description = "Notification Not Found")
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Given ID is malformed")
    public ResponseEntity<NotificationInfo> getNotificationByNotificationId(
        @PathVariable @Valid Integer notificationId) {
        return ok(notificationService.findByNotificationId(notificationId));
    }

    @ApiResponses(value =
        {
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
        }
    )
    @GetMapping(path = "/all-pending") //TODO: FACT-2026
    @Operation(
            method = "GET",
            summary = "Get list of error notifications",
            description = "Get list of all pending notifications"
    )
    public NotificationsResponse getAllPendingNotifications() {
        return mapToNotificationsResponse(notificationService.getAllPendingNotifications());
    }

    /**
     * Add a notification message.
     * This endpoint can be used to add a new error notification message to the notifications table.
     * It will also notify the supplier about this notification.
     * Using this endpoint requires a valid authorisation token.
     * @path /notifications
     * @body {@link NotifyRequest}
     * @header ServiceAuthorisation token to be authenticated by {@link AuthService}
     * @return {@link NotificationInfo} info about the added notification. Including the confirmation
     *          ID that is returned when the supplier is notified.
     */
    @PostMapping
    @Operation(summary = "Add a new notification")
    @ApiResponse(responseCode = CREATED_CODE, description = "Successfully created notification")
    @ApiResponse(responseCode = UNAUTHORISED_CODE, description = UNAUTHORISED_USER)
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "If the notify request body does not pass validation")
    @ApiResponse(responseCode = FAILED_DEPENDENCY_CODE, description = "If there is an error notifying the supplier")
    public ResponseEntity<NotificationInfo> addNotification(
        @RequestHeader(name = "ServiceAuthorization", required = false) String serviceAuthHeader,
        @RequestBody @Validated NotifyRequest notifyRequest) {
        authService.authenticate(serviceAuthHeader);
        return created(URI.create("/notifications"))
            .body(notificationService.saveNotificationMsg(notifyRequest));
    }

    private NotificationsResponse mapToNotificationsResponse(List<Notification> list) {
        List<NotificationInfo> notifications = list.stream()
            .map(this::toNotificationResponse)
            .collect(toList());

        return new NotificationsResponse(notifications);
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
            notification.errorDescription == null
                ?
                ""
                :
                notification.errorDescription.substring(
                    0,
                    min(MAX_ERROR_DESCRIPTION_LENGTH, notification.errorDescription.length())
                ),
            notification.createdAt,
            notification.processedAt,
            notification.status.name()
        );
    }
}
