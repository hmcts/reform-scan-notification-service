package uk.gov.hmcts.reform.notificationservice.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

final class DateFormatter {

    private static final String DATETIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN);

    /**
     * The function `getSimpleDateTime` converts an `Instant` to a formatted date and time
     * string in the Europe/London time zone.
     *
     * @param instant An Instant object representing a specific point in time.
     * @return The method `getSimpleDateTime` returns a formatted string representing the date and time
     *      of the provided `Instant` object in the time zone "Europe/London".
     */
    static String getSimpleDateTime(final Instant instant) {
        return formatter.format(ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/London")));
    }

    private DateFormatter() {
        // utility class constructor
    }
}
