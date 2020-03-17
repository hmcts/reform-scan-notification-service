package uk.gov.hmcts.reform.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.MessageBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.request.incomming.NotificationMsg;

import java.io.IOException;
import java.util.List;

@Service
public class NotificationMessageParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationMessageParser.class);
    private static final String ERROR_CAUSE = "Message Binary data is null";

    private final ObjectMapper objectMapper;

    public NotificationMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationMsg parse(MessageBody messageBody) {
        try {
            NotificationMsg notificationMsg =
                objectMapper.readValue(getBinaryData(messageBody), NotificationMsg.class);
            LOGGER.info(
                "Parsed notification message, Zip File Name: {}, Error Code: {}, Error Description: {} "
                    + "Jurisdiction: {}, PO Box: {}, Service: {}, Document Control Number: {}",
                notificationMsg.zipFileName,
                notificationMsg.errorCode,
                notificationMsg.errorDescription,
                notificationMsg.jurisdiction,
                notificationMsg.poBox,
                notificationMsg.service,
                notificationMsg.documentControlNumber
            );

            return notificationMsg;
        } catch (IOException exc) {
            LOGGER.error("Notification queue message parse error", exc);
            throw new InvalidMessageException(exc);
        }
    }

    private static byte[] getBinaryData(MessageBody messageBody) {
        List<byte[]> binaryData = messageBody.getBinaryData();
        if (CollectionUtils.isEmpty(binaryData) || binaryData.get(0) == null) {
            throw new InvalidMessageException(ERROR_CAUSE);
        }
        return binaryData.get(0);
    }

}
