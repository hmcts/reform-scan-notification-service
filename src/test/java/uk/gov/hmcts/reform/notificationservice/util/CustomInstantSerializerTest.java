package uk.gov.hmcts.reform.notificationservice.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Calendar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomInstantSerializerTest {
    private CustomInstantSerializer customInstantSerializer;

    @Mock
    private JsonGenerator jsonGenerator;

    @Mock
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        customInstantSerializer = new CustomInstantSerializer();
    }

    @Test
    void serialize_should_call_json_generator() throws Exception {
        // given
        Calendar cal   = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2020);
        cal.set(Calendar.MONTH, Calendar.MARCH);
        cal.set(Calendar.DATE, 23);
        cal.set(Calendar.HOUR_OF_DAY, 13);
        cal.set(Calendar.MINUTE, 17);
        cal.set(Calendar.SECOND, 20);
        cal.set(Calendar.MILLISECOND, 234);
        Instant instant = cal.toInstant();

        // when
        customInstantSerializer.serialize(instant, jsonGenerator, serializerProvider);

        // then
        var dateStringCaptor = ArgumentCaptor.forClass(String.class);
        verify(jsonGenerator).writeString(dateStringCaptor.capture());
        assertThat(dateStringCaptor.getValue()).isEqualTo("2020-03-23T13:17:20.234Z");
    }
}
