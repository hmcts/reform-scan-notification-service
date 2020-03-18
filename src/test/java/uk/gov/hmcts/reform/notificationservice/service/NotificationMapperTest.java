package uk.gov.hmcts.reform.notificationservice.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notificationservice.data.NewNotification;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;

public class NotificationMapperTest {

    private NotificationMapper notificationMapper = new NotificationMapper();

    @Test
    public void should_return_valid_NewNotification_when_NotificationMsg_is_valid() {
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

        NewNotification request = notificationMapper.map(notificationMsg);
        assertThat(request.zipFileName).isEqualTo("zipfile.zip");
        assertThat(request.poBox).isEqualTo("pobox");
        assertThat(request.documentControlNumber).isEqualTo("1342411414214");
        assertThat(request.errorCode).isEqualTo(ErrorCode.ERR_SIG_VERIFY_FAILED);
        assertThat(request.errorDescription).isEqualTo("error description signature not valid");
        assertThat(request.service).isEqualTo("orchestrator");

    }
}