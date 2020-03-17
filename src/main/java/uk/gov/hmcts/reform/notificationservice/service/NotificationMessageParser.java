package uk.gov.hmcts.reform.notificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.servicebus.IMessage;
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

    private static final Logger logger = LoggerFactory.getLogger(NotificationMessageParser.class);
    private static final String ERROR_CAUSE = "Message Binary data is null, Message ID: %s";

    private final ObjectMapper objectMapper;

    public NotificationMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationMsg parse(IMessage message) {
        try {
            NotificationMsg notificationMsg =
                objectMapper.readValue(getBinaryData(message), NotificationMsg.class);
            logger.info(
                "Parsed notification message, Message ID: {}, Zip File Name: {}, Error Code: {}, "
                    + "Jurisdiction: {}, PO Box: {}, Service: {}, Document Control Number: {}",
                message.getMessageId(),
                notificationMsg.zipFileName,
                notificationMsg.errorCode,
                notificationMsg.jurisdiction,
                notificationMsg.poBox,
                notificationMsg.service,
                notificationMsg.documentControlNumber
            );

            return notificationMsg;
        } catch (IOException exc) {
            logger.error("Notification queue message parse error, Message ID :{}", message.getMessageId());
            throw new InvalidMessageException(exc);
        }
    }

    private static byte[] getBinaryData(IMessage message) {
        List<byte[]> binaryData = message.getMessageBody().getBinaryData();
        if (CollectionUtils.isEmpty(binaryData) || binaryData.get(0) == null) {
            throw new InvalidMessageException(String.format(ERROR_CAUSE, message.getMessageId()));
        }
        return binaryData.get(0);
    }

}
