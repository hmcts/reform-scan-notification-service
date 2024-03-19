package uk.gov.hmcts.reform.notificationservice.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;

/**
 * The `CustomInstantSerializer` class in Java serializes `Instant` objects into a simple date and time string using a
 * `DateFormatter` class.
 */
public final class CustomInstantSerializer extends StdSerializer<Instant> {

    CustomInstantSerializer() {
        super(Instant.class);
    }

    /**
     * The serialize method formats an Instant value as a simple date and time string using a DateFormatter class.
     *
     * @param value The `value` parameter is an `Instant` object that represents a point in time in the UTC time zone.
     * @param gen The `gen` parameter in the `serialize` method is an instance of `JsonGenerator`. It is used to
     *            write JSON content, such as strings, numbers, objects, and arrays, to the output stream.
     * @param provider The `provider` parameter in the `serialize` method is an instance
     *                 of `SerializerProvider` class. It provides contextual information and helper methods
     *                 for serialization, such as accessing configuration settings, handling type information,
     *                 and resolving serializers for specific types.
     */
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(DateFormatter.getSimpleDateTime(value));
    }
}
