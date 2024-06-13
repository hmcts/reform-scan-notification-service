package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationMessageMapperTest {

    private final NotificationMessageMapper notificationMessageMapper = new NotificationMessageMapper();

    @Test
    void should_return_valid_NewNotification_when_NotificationMsg_is_valid() {
        NotificationMsg notificationMsg =
            new NotificationMsg(
                "zipfile.zip",
                "divorce",
                "pobox",
                "bulkscan",
                "1342411414214",
                ErrorCode.ERR_SIG_VERIFY_FAILED,
                "error description signature not valid",
                "orchestrator"
            );

        NewNotification request = notificationMessageMapper.map(notificationMsg, "1234", "primary");
        assertThat(request.zipFileName).isEqualTo("zipfile.zip");
        assertThat(request.poBox).isEqualTo("pobox");
        assertThat(request.documentControlNumber).isEqualTo("1342411414214");
        assertThat(request.errorCode).isEqualTo(ErrorCode.ERR_SIG_VERIFY_FAILED);
        assertThat(request.errorDescription).isEqualTo("error description signature not valid");
        assertThat(request.service).isEqualTo("orchestrator");
        assertThat(request.client).isEqualTo("primary");
    }
}
