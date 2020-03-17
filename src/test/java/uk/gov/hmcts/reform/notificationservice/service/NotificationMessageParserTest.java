package uk.gov.hmcts.reform.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.microsoft.azure.servicebus.Message;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import static com.microsoft.azure.servicebus.MessageBody.fromSequenceData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NotificationMessageParserTest {

    private ObjectMapper objectMapper = new ObjectMapper();

    private final NotificationMessageParser notificationMsgParser =
        new NotificationMessageParser(objectMapper);

    @Test
    public void should_return_valid_notificationMessage_when_queue_message_is_valid() throws JSONException {
        NotificationMsg expected = new NotificationMsg(
            "fileName.zip",
            "divorce",
            "pobox",
            "1234567890123456",
            ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
            "size too big",
            "orchestrator"
        );

        NotificationMsg notificationMessage =
            notificationMsgParser.parse(
                new Message(
                    notificationMessageAsJsonString(
                        "fileName.zip",
                        "divorce",
                        "pobox",
                        "1234567890123456",
                        ErrorCode.ERR_FILE_LIMIT_EXCEEDED,
                        "size too big",
                        "orchestrator")
                        .getBytes()
                )
            );

        assertThat(notificationMessage).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    public void should_throw_invalidMessageException_when_queue_message_is_invalid() {
        assertThatThrownBy(() -> notificationMsgParser.parse(new Message("parse exception")))
            .isInstanceOf(InvalidMessageException.class);
    }

    @Test
    public void should_throw_InvalidMessageException_when_queue_message_is_null() {
        Message emptyMessage = new Message(fromSequenceData(ImmutableList.of(ImmutableList.of(new Object()))));
        assertThatThrownBy(() -> notificationMsgParser.parse(emptyMessage))
            .isInstanceOf(InvalidMessageException.class)
            .hasMessage("Message Binary data is null, Message ID: " + emptyMessage.getMessageId());
    }

    private static String notificationMessageAsJsonString(
        String zipFileName,
        String jurisdiction,
        String poBox,
        String documentControlNumber,
        ErrorCode errorCode,
        String errorDescription,
        String service)
        throws JSONException {
        return new JSONObject()
            .put("zip_file_name", zipFileName)
            .put("jurisdiction", jurisdiction)
            .put("po_box", poBox)
            .put("document_control_number", documentControlNumber)
            .put("error_code", errorCode)
            .put("error_description", errorDescription)
            .put("service", service)
            .toString();
    }
}
