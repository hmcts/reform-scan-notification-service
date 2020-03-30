package uk.gov.hmcts.reform.notificationservice;

import com.microsoft.azure.servicebus.MessageBody;
import org.springframework.http.MediaType;

class QueueMessageDetails {

    final String messageId;
    final MessageBody messageBody;
    final String contentType;
    final String zipFileName;
    final String service;

    QueueMessageDetails(String messageId, MessageBody messageBody, String zipFileName, String service) {
        this.messageId = messageId;
        this.messageBody = messageBody;
        this.contentType = MediaType.APPLICATION_JSON_VALUE;
        this.zipFileName = zipFileName;
        this.service = service;
    }
}
