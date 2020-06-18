package uk.gov.hmcts.reform.notificationservice.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/London")));
    }

    private DateFormatter() {
        // utility class constructor
    }
}
