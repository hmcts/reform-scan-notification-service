package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationMessageParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private final NotificationMessageParser notificationMsgParser =
        new NotificationMessageParser(objectMapper);

    @Test
    void should_return_valid_notificationMessage_when_queue_message_is_valid() throws JSONException {
        NotificationMsg expected = new NotificationMsg(
            "fileName.zip",
            "divorce",
            "pobox",
            "divorce",
            "1234567890123456",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "size too big",
            "orchestrator"
        );

        NotificationMsg notificationMessage =
            notificationMsgParser.parse(
                BinaryData.fromBytes(
                    notificationMessageAsJsonString(
                        "fileName.zip",
                        "divorce",
                        "pobox",
                        "divorce",
                        "1234567890123456",
                        ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
                        "size too big",
                        "orchestrator"
                    ).getBytes()
                )
            );

        assertThat(notificationMessage).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void should_throw_invalidMessageException_when_queue_message_is_invalid() {
        BinaryData messageBody = BinaryData.fromBytes("parse exception".getBytes());

        assertThatThrownBy(() -> notificationMsgParser.parse(messageBody))
                .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    void should_throw_InvalidMessageException_when_queue_message_is_null() {
        BinaryData nullBinaryData = BinaryData.fromObject((new Object()));
        assertThatThrownBy(() -> notificationMsgParser.parse(nullBinaryData))
            .isInstanceOf(InvalidMessageException.class);
    }

    private static String notificationMessageAsJsonString(
        String zipFileName,
        String jurisdiction,
        String poBox,
        String container,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        String service
    ) throws JSONException {
        return new JSONObject()
            .put("zip_file_name", zipFileName)
            .put("jurisdiction", jurisdiction)
            .put("po_box", poBox)
            .put("container", container)
            .put("document_control_number", documentControlNumber)
            .put("error_code", errorCode)
            .put("error_description", errorDescription)
            .put("service", service)
            .toString();
    }
}
