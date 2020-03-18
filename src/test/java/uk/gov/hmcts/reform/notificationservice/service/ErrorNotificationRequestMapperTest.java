package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notificationservice.clients.ErrorNotificationRequest;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;

public class ErrorNotificationRequestMapperTest {

    private ErrorNotificationRequestMapper errorNotificationRequestMapper = new ErrorNotificationRequestMapper();

    @Test
    public void should_return_valid_ErrorNotificationRequest_when_NotificationMsg_is_valid() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "zipfile.zip",
                "divorce",
                "pobox",
                "1342411414214",
                ErrorCode.ERR_SIG_VERIFY_FAILED,
                "error description signature not valid",
                "orchestrator"
            );

        ErrorNotificationRequest request = errorNotificationRequestMapper.map(notificationMsg);
        assertThat(request.errorCode).isEqualTo("ERR_SIG_VERIFY_FAILED");
        assertThat(request.errorDescription).isEqualTo("error description signature not valid");
        assertThat(request.poBox).isEqualTo("pobox");
        assertThat(request.referenceId).isNull();
        assertThat(request.zipFileName).isEqualTo("zipfile.zip");
    }
}