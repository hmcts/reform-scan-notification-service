package uk.gov.hmcts.reform.notificationservice.controller;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.service.AuthService;
import uk.gov.hmcts.reform.notificationservice.service.NotificationService;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static java.time.Instant.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    protected NotificationService notificationService;

    @MockBean
    protected AuthService authService;

    private static final String PRIMARY_CLIENT = "primary";

    @Test
    void should_get_notifications_by_file_name_and_service() throws Exception {
        final String fileName = "zip_file_name.zip";
        final String auth = "auth";
        final String service = "service";
        Instant instant
            = Instant.parse("2020-03-23T13:17:20.00Z");
        final String instantString = "2020-03-23T13:17:20";

        var notification1 = new Notification(
            1L,
            "confirmation-id-1",
            fileName,
            "po_box1",
            "container",
            service,
            "DCN1",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile1",
            instant,
            instant,
            NotificationStatus.SENT,
            "messageId1",
            PRIMARY_CLIENT
        );
        var notification2 = new Notification(
            2L,
            "confirmation-id-2",
            fileName,
            "po_box2",
            "container",
            service,
            "DCN2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            null,
            instant,
            instant,
            NotificationStatus.MANUALLY_HANDLED,
            "messageId2",
            PRIMARY_CLIENT
        );

        var trimmedErrorDesc = "Start_" + RandomStringUtils.randomAlphabetic(1014) + "_end";
        var notification3 = new Notification(
            3L,
            "confirmation-id-3",
            fileName,
            "po_box3",
            "container",
            service,
            "DCN3",
            ErrorCode.ERR_PAYMENTS_DISABLED,
            trimmedErrorDesc + "_should_not_seen",
            instant,
            instant,
            NotificationStatus.SENT,
            "messageId3",
            PRIMARY_CLIENT
        );
        given(authService.authenticate(auth)).willReturn(service);
        given(notificationService.findByFileNameAndService(fileName, service))
            .willReturn(asList(notification1, notification2, notification3));

        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", auth)
                    .queryParam("file_name", fileName)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", is(3)))
            .andExpect(jsonPath("$.sentNotificationsCount", is(2)))
            .andExpect(jsonPath("$.pendingNotificationsCount", is(0)))
            .andExpect(jsonPath("$.notifications", hasSize(3)))
            .andExpect(jsonPath("$.notifications[0].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[0].id").value(notification1.id))
            .andExpect(jsonPath("$.notifications[0].confirmation_id").value(notification1.confirmationId))
            .andExpect(jsonPath("$.notifications[0].zip_file_name").value(notification1.zipFileName))
            .andExpect(jsonPath("$.notifications[0].po_box").value(notification1.poBox))
            .andExpect(jsonPath("$.notifications[0].container").value(notification1.container))
            .andExpect(jsonPath("$.notifications[0].service").value(notification1.service))
            .andExpect(jsonPath("$.notifications[0].document_control_number")
                           .value(notification1.documentControlNumber))
            .andExpect(jsonPath("$.notifications[0].error_code").value(notification1.errorCode.name()))
            .andExpect(jsonPath("$.notifications[0].error_description").value(notification1.errorDescription))
            .andExpect(jsonPath("$.notifications[0].created_at").value(instantString))
            .andExpect(jsonPath("$.notifications[0].processed_at").value(instantString))
            .andExpect(jsonPath("$.notifications[0].status").value(notification1.status.name()))
            .andExpect(jsonPath("$.notifications[1].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[1].id").value(notification2.id))
            .andExpect(jsonPath("$.notifications[1].confirmation_id").value(notification2.confirmationId))
            .andExpect(jsonPath("$.notifications[1].zip_file_name").value(notification2.zipFileName))
            .andExpect(jsonPath("$.notifications[1].po_box").value(notification2.poBox))
            .andExpect(jsonPath("$.notifications[1].container").value(notification2.container))
            .andExpect(jsonPath("$.notifications[1].service").value(notification2.service))
            .andExpect(jsonPath("$.notifications[1].document_control_number")
                           .value(notification2.documentControlNumber))
            .andExpect(jsonPath("$.notifications[1].error_code").value(notification2.errorCode.name()))
            .andExpect(jsonPath("$.notifications[1].error_description").value(""))
            .andExpect(jsonPath("$.notifications[1].created_at").value(instantString))
            .andExpect(jsonPath("$.notifications[1].processed_at").value(instantString))
            .andExpect(jsonPath("$.notifications[1].status").value(notification2.status.name()))
            .andExpect(jsonPath("$.sentNotificationsCount", is(2)))
            .andExpect(jsonPath("$.pendingNotificationsCount", is(0)))
            .andExpect(jsonPath("$.notifications[2].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[2].id").value(notification3.id))
            .andExpect(jsonPath("$.notifications[2].confirmation_id").value(notification3.confirmationId))
            .andExpect(jsonPath("$.notifications[2].zip_file_name").value(notification3.zipFileName))
            .andExpect(jsonPath("$.notifications[2].po_box").value(notification3.poBox))
            .andExpect(jsonPath("$.notifications[2].container").value(notification3.container))
            .andExpect(jsonPath("$.notifications[2].service").value(notification3.service))
            .andExpect(jsonPath("$.notifications[2].document_control_number")
                           .value(notification3.documentControlNumber))
            .andExpect(jsonPath("$.notifications[2].error_code").value(notification3.errorCode.name()))
            .andExpect(jsonPath("$.notifications[2].error_description").value(trimmedErrorDesc))
            .andExpect(jsonPath("$.notifications[2].created_at").value(instantString))
            .andExpect(jsonPath("$.notifications[2].processed_at").value(instantString))
            .andExpect(jsonPath("$.notifications[2].status").value(notification3.status.name()));
    }

    @Test
    void should_return_empty_list_if_no_notifications_found_for_given_file_name_and_service() throws Exception {
        final String fileName = "hello.zip";
        final String auth = "auth";
        final String service = "service";

        given(authService.authenticate(auth)).willReturn(service);
        given(notificationService.findByFileNameAndService(fileName, service)).willReturn(emptyList());

        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", auth)
                    .queryParam("file_name", fileName)
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", is(0)))
            .andExpect(jsonPath("$.pendingNotificationsCount", is(0)))
            .andExpect(jsonPath("$.sentNotificationsCount", is(0)))
            .andExpect(jsonPath("$.notifications", hasSize(0)))
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_service_authorization_header_missing() throws Exception {
        final String fileName = "hello.zip";
        given(authService.authenticate(null)).willCallRealMethod();

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", fileName)
            )
            .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_invalid_token_exception() throws Exception {
        final String fileName = "hello.zip";
        final String auth = "auth";

        given(authService.authenticate(auth)).willThrow(new InvalidTokenException("msg"));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", fileName)
                    .header("ServiceAuthorization", auth)
            )
            .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_service_exception() throws Exception {
        final String fileName = "hello.zip";
        final String auth = "auth";

        given(authService.authenticate(auth)).willThrow(new ServiceException("msg", new RuntimeException()));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", fileName)
                    .header("ServiceAuthorization", auth)
            )
            .andExpect(status().isInternalServerError());
    }

    @Test
    void should_get_notifications_by_date() throws Exception {

        LocalDate date = LocalDate.now();
        Instant instantNow = date.atStartOfDay(ZoneOffset.UTC).toInstant();

        String instantNowStr = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.of("Europe/London"))
            .format(instantNow);

        var notification1 = new Notification(
            1L,
            "confirmation-id-1",
            "zip_file_name_123.zip",
            "po_box1",
            "container",
            "bulk_scan",
            "DCN1",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile1",
            instantNow,
            instantNow,
            NotificationStatus.SENT,
            "messageId1",
            PRIMARY_CLIENT
        );
        var notification2 = new Notification(
            2L,
            "confirmation-id-2",
            "file_name_1.zip",
            "po_box_2",
            "container_x",
            "service_1",
            "DCN2",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "invalid metafile_2",
            instantNow,
            instantNow,
            NotificationStatus.SENT,
            "messageId2",
            PRIMARY_CLIENT
        );

        given(notificationService.findByDate(date))
            .willReturn(asList(notification1, notification2));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("date", date.toString())
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", is(2)))
            .andExpect(jsonPath("$.notifications", hasSize(2)))
            .andExpect(jsonPath("$.sentNotificationsCount", is(2)))
            .andExpect(jsonPath("$.pendingNotificationsCount", is(0)))
            .andExpect(jsonPath("$.notifications[0].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[0].confirmation_id").value(notification1.confirmationId))
            .andExpect(jsonPath("$.notifications[0].zip_file_name").value(notification1.zipFileName))
            .andExpect(jsonPath("$.notifications[0].po_box").value(notification1.poBox))
            .andExpect(jsonPath("$.notifications[0].container").value(notification1.container))
            .andExpect(jsonPath("$.notifications[0].service").value(notification1.service))
            .andExpect(jsonPath("$.notifications[0].document_control_number")
                .value(notification1.documentControlNumber))
            .andExpect(jsonPath("$.notifications[0].error_code").value(notification1.errorCode.name()))
            .andExpect(jsonPath("$.notifications[0].error_description").value(notification1.errorDescription))
            .andExpect(jsonPath("$.notifications[0].created_at").value(instantNowStr))
            .andExpect(jsonPath("$.notifications[0].processed_at").value(instantNowStr))
            .andExpect(jsonPath("$.notifications[0].status").value(notification1.status.name()))
            .andExpect(jsonPath("$.notifications[1].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[1].confirmation_id").value(notification2.confirmationId))
            .andExpect(jsonPath("$.notifications[1].zip_file_name").value(notification2.zipFileName))
            .andExpect(jsonPath("$.notifications[1].po_box").value(notification2.poBox))
            .andExpect(jsonPath("$.notifications[1].container").value(notification2.container))
            .andExpect(jsonPath("$.notifications[1].service").value(notification2.service))
            .andExpect(jsonPath("$.notifications[1].document_control_number")
                .value(notification2.documentControlNumber))
            .andExpect(jsonPath("$.notifications[1].error_code").value(notification2.errorCode.name()))
            .andExpect(jsonPath("$.notifications[1].error_description").value(notification2.errorDescription))
            .andExpect(jsonPath("$.notifications[1].created_at").value(instantNowStr))
            .andExpect(jsonPath("$.notifications[1].processed_at").value(instantNowStr))
            .andExpect(jsonPath("$.notifications[1].status").value(notification2.status.name()));
    }

    @Test
    void should_return_400_when_date_param_is_not_valid() throws Exception {
        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("date", "3232")
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void should_get_notifications_by_zip_file_name() throws Exception {

        String zipFileName = "zip_file_name_123.zip";
        var notification1 = new Notification(
                1L,
                "confirmation-id-1",
                zipFileName,
                "po_box1",
                "container",
                "bulk_scan",
                "DCN1",
                ErrorCode.ERR_METAFILE_INVALID,
                "invalid metafile1",
                now(),
                now(),
                NotificationStatus.SENT,
                "messageId1",
                PRIMARY_CLIENT
        );

        given(notificationService.findByZipFileName(zipFileName))
                .willReturn(singletonList(notification1));

        mockMvc
                .perform(
                        get("/notifications")
                                .queryParam("zip_file_name", zipFileName)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(1)))
                .andExpect(jsonPath("$.notifications", hasSize(1)))
                .andExpect(jsonPath("$.sentNotificationsCount", is(1)))
                .andExpect(jsonPath("$.pendingNotificationsCount", is(0)))
                .andExpect(jsonPath("$.notifications[0].id").isNotEmpty())
                .andExpect(jsonPath("$.notifications[0].confirmation_id").value(notification1.confirmationId))
                .andExpect(jsonPath("$.notifications[0].zip_file_name").value(notification1.zipFileName))
                .andExpect(jsonPath("$.notifications[0].po_box").value(notification1.poBox))
                .andExpect(jsonPath("$.notifications[0].container").value(notification1.container))
                .andExpect(jsonPath("$.notifications[0].service").value(notification1.service))
                .andExpect(jsonPath("$.notifications[0].document_control_number")
                        .value(notification1.documentControlNumber));
    }

    @Test
    void should_get_all_pending_notifications() throws Exception {

        var notification1 = new Notification(
                1L,
                "confirmation-id-1",
                "zip_file_name_123.zip",
                "po_box1",
                "container",
                "bulk_scan",
                "DCN1",
                ErrorCode.ERR_METAFILE_INVALID,
                "invalid metafile1",
                now(),
                now(),
                NotificationStatus.PENDING,
                "messageId1",
                PRIMARY_CLIENT
        );
        var notification2 = new Notification(
                2L,
                "confirmation-id-2",
                "zip_file_name_234.zip",
                "po_box1",
                "container",
                "bulk_scan",
                "DCN2",
                ErrorCode.ERR_METAFILE_INVALID,
                "invalid metafile2",
                now(),
                now(),
                NotificationStatus.PENDING,
                "messageId2",
                PRIMARY_CLIENT
        );

        given(notificationService.getAllPendingNotifications())
                .willReturn(asList(notification1, notification2));

        mockMvc
                .perform(
                        get("/notifications/all-pending")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count", is(2)))
                .andExpect(jsonPath("$.notifications", hasSize(2)))
                .andExpect(jsonPath("$.sentNotificationsCount", is(0)))
                .andExpect(jsonPath("$.pendingNotificationsCount", is(2)))
                .andExpect(jsonPath("$.notifications[0].id").value(notification1.id))
                .andExpect(jsonPath("$.notifications[0].confirmation_id").value(notification1.confirmationId))
                .andExpect(jsonPath("$.notifications[0].zip_file_name").value(notification1.zipFileName))
                .andExpect(jsonPath("$.notifications[0].po_box").value(notification1.poBox))
                .andExpect(jsonPath("$.notifications[0].container").value(notification1.container))
                .andExpect(jsonPath("$.notifications[0].service").value(notification1.service))
                .andExpect(jsonPath("$.notifications[0].document_control_number")
                        .value(notification1.documentControlNumber))
                .andExpect(jsonPath("$.notifications[1].id").value(notification2.id))
                .andExpect(jsonPath("$.notifications[1].confirmation_id").value(notification2.confirmationId))
                .andExpect(jsonPath("$.notifications[1].zip_file_name").value(notification2.zipFileName))
                .andExpect(jsonPath("$.notifications[1].po_box").value(notification2.poBox))
                .andExpect(jsonPath("$.notifications[1].container").value(notification2.container))
                .andExpect(jsonPath("$.notifications[1].service").value(notification2.service))
                .andExpect(jsonPath("$.notifications[1].document_control_number")
                        .value(notification2.documentControlNumber));
    }
}
