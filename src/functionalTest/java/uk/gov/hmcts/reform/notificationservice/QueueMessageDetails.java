package uk.gov.hmcts.reform.notificationservice;

import com.azure.core.util.BinaryData;
import org.springframework.http.MediaType;

//TODO: FACT-2026 - Whole class can go
class QueueMessageDetails {

    final String messageId;
    final BinaryData messageBody;
    final String contentType;
    final String zipFileName;
    final String service;

    QueueMessageDetails(String messageId, BinaryData messageBody, String zipFileName, String service) {
        this.messageId = messageId;
        this.messageBody = messageBody;
        this.contentType = MediaType.APPLICATION_JSON_VALUE;
        this.zipFileName = zipFileName;
        this.service = service;
    }
}
