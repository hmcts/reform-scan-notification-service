package uk.gov.hmcts.reform.notificationservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.reform.authorisation.exceptions.InvalidTokenException;
import uk.gov.hmcts.reform.authorisation.exceptions.ServiceException;
import uk.gov.hmcts.reform.notificationservice.data.Notification;
import uk.gov.hmcts.reform.notificationservice.data.NotificationStatus;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.time.Instant;
import java.util.Calendar;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@AutoConfigureMockMvc
@SpringBootTest
public class NotificationControllerTest extends ControllerTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() {
        mockMvc = webAppContextSetup(wac).build();
    }

    @Test
    void should_get_notifications_by_file_name_and_service() throws Exception {
        final String fileName = "zip_file_name.zip";
        final String auth = "auth";
        final String service = "service";

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DATE, 23);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 17);
        cal.set(Calendar.SECOND, 20);
        cal.set(Calendar.MILLISECOND, 234);
        Instant instant = cal.toInstant();
        final String instantString = "2020-03-23T13:17:20.234Z";

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
            NotificationStatus.SENT
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
            "invalid metafile2",
            instant,
            instant,
            NotificationStatus.SENT
        );

        given(tokenValidator.getServiceName(auth)).willReturn(service);
        given(notificationService.findByFileNameAndService(fileName, service))
            .willReturn(asList(notification1, notification2));


        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", auth)
                    .queryParam("file_name", fileName)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", is(2)))
            .andExpect(jsonPath("$.notifications", hasSize(2)))
            .andExpect(jsonPath("$.notifications[0].id").isNotEmpty())
            .andExpect(jsonPath("$.notifications[0].confirmation_id").value(notification1.confirmationId))
            .andExpect(jsonPath("$.notifications[0].zip_file_name").value(notification1.zipFileName))
            .andExpect(jsonPath("$.notifications[0].po_box").value(notification1.poBox))
            .andExpect(jsonPath("$.notifications[0].container").value(notification1.container))
            .andExpect(jsonPath("$.notifications[0].service").value(notification1.service))
            .andExpect(jsonPath("$.notifications[0].document_control_number")
                           .value(notification1.documentControlNumber))
            .andExpect(jsonPath("$.notifications[0].error_code").value(notification1.errorCode.name()))
            .andExpect(jsonPath("$.notifications[0].created_at").value(instantString))
            .andExpect(jsonPath("$.notifications[0].processed_at").value(instantString))
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
            .andExpect(jsonPath("$.notifications[1].created_at").value(instantString))
            .andExpect(jsonPath("$.notifications[1].processed_at").value(instantString))
            .andExpect(jsonPath("$.notifications[1].status").value(notification2.status.name()))
        ;
    }

    @Test
    void should_return_empty_list_if_no_notifications_found_for_given_file_name_and_service() throws Exception {
        final String fileName = "hello.zip";
        final String auth = "auth";
        final String service = "service";

        given(tokenValidator.getServiceName(auth)).willReturn(service);
        given(notificationService.findByFileNameAndService(fileName, service)).willReturn(emptyList());

        mockMvc
            .perform(
                get("/notifications")
                    .header("ServiceAuthorization", auth)
                    .queryParam("file_name", fileName)
            )
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.count", is(0)))
            .andExpect(jsonPath("$.notifications", hasSize(0)))
        ;
    }

    @Test
    void should_respond_with_unauthenticated_if_service_authorization_header_missing() throws Exception {
        final String fileName = "hello.zip";

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

        given(tokenValidator.getServiceName(auth)).willThrow(new InvalidTokenException("msg"));

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

        given(tokenValidator.getServiceName(auth)).willThrow(new ServiceException("msg", new RuntimeException()));

        mockMvc
            .perform(
                get("/notifications")
                    .queryParam("file_name", fileName)
                    .header("ServiceAuthorization", auth)
            )
            .andExpect(status().isInternalServerError())
        ;
    }
}
