package uk.gov.hmcts.reform.notificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.notificationservice.config.SecondaryClientJurisdictionsConfig;
import uk.gov.hmcts.reform.notificationservice.data.NotificationRepository;
import uk.gov.hmcts.reform.notificationservice.model.in.NotificationMsg;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

@Service
public class NotificationMessageHandler {
    private static final Logger log = LoggerFactory.getLogger(NotificationMessageHandler.class);

    private final NotificationMessageMapper notificationMessageMapper;
    private final NotificationRepository notificationRepository;
    private final String[] secondaryClientJurisdictions;

    @Autowired
    public NotificationMessageHandler(
        NotificationMessageMapper notificationMessageMapper,
        NotificationRepository notificationRepository,
        SecondaryClientJurisdictionsConfig secondaryClientJurisdictions
    ) {
        this.notificationMessageMapper = notificationMessageMapper;
        this.notificationRepository = notificationRepository;
        this.secondaryClientJurisdictions = secondaryClientJurisdictions.getJurisdictionList();
    }

    public void handleNotificationMessage(NotificationMsg notificationMsg, String messageId) {
        log.info("Handle notification message, Zip File: {}", notificationMsg.zipFileName);

        // Cater for the possibility of the jurisdiction being not present. If it is, set it to primary
        String jurisdiction = Objects.requireNonNullElse(notificationMsg.jurisdiction, "").toLowerCase(Locale.ROOT);
        String client = Arrays.asList(secondaryClientJurisdictions).contains(jurisdiction) ? "secondary" : "primary";

        var newNotification = notificationMessageMapper
            .map(notificationMsg, messageId, client);

        long id = notificationRepository.insert(newNotification);
        log.info(
            "Handle notification message successful: Zip File: {}, "
                + "inserted Notification ID: {}, sent to client: {}, jurisdiction: {}",
            notificationMsg.zipFileName,
            id,
            client,
            jurisdiction
        );
    }
}
