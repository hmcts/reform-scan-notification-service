package uk.gov.hmcts.reform.notificationservice.service;

import com.azure.core.util.BinaryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.exception.InvalidMessageException;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import java.io.IOException;

@Service
public class NotificationMessageParser {

    private static final Logger logger = LoggerFactory.getLogger(NotificationMessageParser.class);

    private final ObjectMapper objectMapper;

    public NotificationMessageParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public NotificationMsg parse(BinaryData messageBody) {
        try {
            NotificationMsg notificationMsg =
                objectMapper.readValue(messageBody.toString(), NotificationMsg.class);
            logger.info(
                "Parsed notification message, Zip File Name: {}, Error Code: {}, "
                    + "Jurisdiction: {}, PO Box: {}, Container {}, Service: {}, Document Control Number: {}",
                notificationMsg.zipFileName,
                notificationMsg.errorCode,
                notificationMsg.jurisdiction,
                notificationMsg.poBox,
                notificationMsg.container,
                notificationMsg.service,
                notificationMsg.documentControlNumber
            );

            return notificationMsg;
        } catch (IOException exc) {
            throw new InvalidMessageException(exc);
        }
    }

}
