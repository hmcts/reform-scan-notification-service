package uk.gov.hmcts.reform.notificationservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.exception.NotFoundException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotifyRequest;
import uk.gov.hmcts.reform.notificationservice.model.out.NotificationInfo;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.notificationservice.data.NotificationStatus.SENT;

@WebMvcTest(controllers = NotificationController.class)
public class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    protected NotificationService notificationService;

    @MockBean
    protected AuthService authService;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PRIMARY_CLIENT = "primary";
    private static final String PATH = "/notifications";
    private static final Integer NOTIFICATION_ID = 1;
    private static final String AUTH = "auth";
    private static final String SERVICE = "service";
    private static final String FILENAME = "zip_file_name.zip";
    private static final String FILENAME2 = "hello.zip";



    @Test
    void should_get_notifications_by_file_name_and_service() throws Exception {
        Instant instant
            = Instant.parse("2020-03-23T13:17:20.00Z");
        final String instantString = "2020-03-23T13:17:20";

        var notification1 = new Notification(
            1L,
            "confirmation-id-1",
            FILENAME,
            "po_box1",
            "container",
            SERVICE,
            "DCN1",
            ErrorCode.ERR_METAFILE_INVALID,
            "invalid metafile1",
            instant,
            instant,
            SENT,
            "messageId1",
            PRIMARY_CLIENT
        );
        var notification2 = new Notification(
            2L,
            "confirmation-id-2",
            FILENAME,
            "po_box2",
            "container",
            SERVICE,
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
            FILENAME,
            "po_box3",
            "container",
            SERVICE,
            "DCN3",
            ErrorCode.ERR_PAYMENTS_DISABLED,
            trimmedErrorDesc + "_should_not_seen",
            instant,
            instant,
            SENT,
            "messageId3",
            PRIMARY_CLIENT
        );
        given(authService.authenticate(AUTH)).willReturn(SERVICE);
        given(notificationService.findByFileNameAndService(FILENAME, SERVICE))
            .willReturn(asList(notification1, notification2, notification3));

        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", AUTH)
                    .queryParam("file_name", FILENAME)
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

        given(authService.authenticate(AUTH)).willReturn(SERVICE);
        given(notificationService.findByFileNameAndService(FILENAME2, SERVICE)).willReturn(emptyList());

        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", AUTH)
                    .queryParam("file_name", FILENAME2)
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
        given(authService.authenticate(null)).willCallRealMethod();

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", FILENAME2)
            )
            .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_invalid_token_exception() throws Exception {
        given(authService.authenticate("notvalid")).willThrow(new InvalidTokenException("msg"));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", FILENAME2)
                    .header("ServiceAuthorization", "notvalid")
            )
            .andExpect(status().isUnauthorized())
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_service_exception() throws Exception {
        given(authService.authenticate(AUTH)).willThrow(new ServiceException("msg", new RuntimeException()));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", FILENAME2)
                    .header("ServiceAuthorization", AUTH)
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
            SENT,
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
            SENT,
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
                SENT,
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

    @Test
    void should_get_notifications_by_notification_id() throws Exception {
        var notificationInfo = new NotificationInfo(
            NOTIFICATION_ID,
            "confirmation-id-1",
            "zip_file_name_123.zip",
            "po_box1",
            "container",
            "bulk_scan",
            "33245532341",
            ErrorCode.ERR_METAFILE_INVALID.toString(),
            "invalid metafile1",
            now(),
            now(),
            SENT.toString()
        );

        when(notificationService.findByNotificationId(NOTIFICATION_ID)).thenReturn(notificationInfo);

        final String notificationJson = OBJECT_MAPPER.writeValueAsString(notificationInfo);
        mockMvc.perform(get(PATH + "/" + NOTIFICATION_ID))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(notificationJson));
    }

    @Test
    void should_not_return_notification_by_id_if_not_found() throws Exception {
        when(notificationService.findByNotificationId(2)).thenThrow(new NotFoundException("Notification not found with ID: " + 2));

        mockMvc.perform(get(PATH + "/" + 2))
            .andExpect(status().isNotFound());
    }

    @Test
    void should_not_return_notification_if_id_is_not_integer() throws Exception {
        mockMvc.perform(get(PATH + "/" + 2.4))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid number. You must use a whole number e.g. not decimals like 13.0 and not letters"));
    }

    @Test
    void should_save_new_notification() throws Exception {
        var notificationMsg = new NotifyRequest(
            "zip_file_name_123.zip",
            "civil",
            "14620",
            "sscs",
            "36222789074101144",
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "invalid signature - gif reactions not allowed",
            "reform_scan_notification_tests"
        );

        var notificationInfo = new NotificationInfo(
            NOTIFICATION_ID,
            "exela id",
            "zip_file_name_123.zip",
            "14620",
            "sscs",
            "36222789074101144",
            "invalid signature - gif reactions not allowed",
            ErrorCode.ERR_SIG_VERIFY_FAILED.toString(),
            "invalid signature - gif reactions not allowed",
            Instant.now(),
            Instant.now(),
            SENT.toString()
        );

        when(notificationService.saveNotificationMsg(notificationMsg)).thenReturn(notificationInfo);
        when(authService.authenticate(AUTH)).thenReturn(SERVICE);

        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String notificationMsgJson = OBJECT_MAPPER.writeValueAsString(notificationMsg);
        final String notificationInfoJson = OBJECT_MAPPER.writeValueAsString(notificationInfo);

        mockMvc.perform(post(PATH)
                            .header("ServiceAuthorization", AUTH)
                            .content(notificationMsgJson)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(notificationInfoJson))
            .andReturn();
    }

    @Test
    void should_not_save_notification_if_authentication_not_present() throws Exception {
        var notificationMsg = new NotifyRequest(
            "zip_file_name_123.zip",
            "civil",
            "14620",
            "sscs",
            "36222789074101144",
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "invalid signature - gif reactions not allowed",
            "reform_scan_notification_tests"
        );
        given(authService.authenticate(null)).willCallRealMethod();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String notificationInfoJson = OBJECT_MAPPER.writeValueAsString(notificationMsg);

        mockMvc.perform(post(PATH)
                            .content(notificationInfoJson)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnauthorized())
            .andReturn();
    }

    @Test
    void should_not_save_notification_if_authentication_not_valid() throws Exception {
        var notificationMsg = new NotifyRequest(
            "zip_file_name_123.zip",
            "civil",
            "14620",
            "sscs",
            "36222789074101144",
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "invalid signature - gif reactions not allowed",
            "reform_scan_notification_tests"
        );
        given(authService.authenticate("imnotvalid")).willThrow(new InvalidTokenException("msg"));
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String notificationInfoJson = OBJECT_MAPPER.writeValueAsString(notificationMsg);

        mockMvc.perform(post(PATH)
                            .header("ServiceAuthorization", "imnotvalid")
                            .content(notificationInfoJson)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isUnauthorized())
            .andReturn();
    }

    @Test
    void should_apply_constraints_on_notification_request_body() throws Exception {
        String auth = "auth";
        var notificationMsg = new NotifyRequest(
            "",
            "jurisdiction",
            "14620",
            null,
            "36222789074101144",
            null,
            "",
            null
        );

        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String notificationInfoJson = OBJECT_MAPPER.writeValueAsString(notificationMsg);

        mockMvc.perform(post(PATH)
                            .header("ServiceAuthorization", auth)
                            .content(notificationInfoJson)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errorCode").value("An error code must be provided"))
            .andExpect(jsonPath("$.container").value("must not be blank"))
            .andExpect(jsonPath("$.zipFileName").value("must not be blank"))
            .andExpect(jsonPath("$.errorDescription").value("must not be blank"))
            .andExpect(jsonPath("$.service").value("must not be blank"))
            .andReturn();
    }

    @Test
    void should_not_put_constraints_on_certain_fields() throws Exception {
        var notificationMsg = new NotifyRequest(
            "zip_file_name_123.zip",
            null,
            null,
            "sscs",
            null,
            ErrorCode.ERR_SIG_VERIFY_FAILED,
            "invalid signature - gif reactions not allowed",
            "reform_scan_notification_tests"
        );

        var notificationInfo = new NotificationInfo(
            NOTIFICATION_ID,
            "exela id",
            "zip_file_name_123.zip",
            "14620",
            "sscs",
            "36222789074101144",
            "invalid signature - gif reactions not allowed",
            ErrorCode.ERR_SIG_VERIFY_FAILED.toString(),
            "invalid signature - gif reactions not allowed",
            Instant.now(),
            Instant.now(),
            SENT.toString()
        );

        when(notificationService.saveNotificationMsg(notificationMsg)).thenReturn(notificationInfo);
        when(authService.authenticate(AUTH)).thenReturn(SERVICE);

        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        final String notificationMsgJson = OBJECT_MAPPER.writeValueAsString(notificationMsg);
        final String notificationInfoJson = OBJECT_MAPPER.writeValueAsString(notificationInfo);

        mockMvc.perform(post(PATH)
                            .header("ServiceAuthorization", AUTH)
                            .content(notificationMsgJson)
                            .contentType(MediaType.APPLICATION_JSON_VALUE)
                            .accept(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string(notificationInfoJson))
            .andReturn();
    }
}
