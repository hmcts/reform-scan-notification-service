package uk.gov.hmcts.reform.notificationservice.data;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * The `NotificationMapper` class in Java implements `RowMapper` to map database
 * query results to `Notification` objects, including a method to convert `Timestamp` to `Instant`.
 */
@Component
public class NotificationMapper implements RowMapper<Notification> {

    /**
     * The `mapRow` function maps a row from a ResultSet to a Notification object in Java.
     *
     * @param rs The `rs` parameter in the `mapRow` method is a `ResultSet` object, which represents a set of
     *           results of a database query. It provides methods to retrieve data from the result set based
     *           on column names or indices.
     * @param rowNum The `rowNum` parameter in the `mapRow` method represents the current row number being
     *               processed by the ResultSet. It is an integer value that indicates the position of the
     *               current row within the result set.
     * @return A `Notification` object is being returned, which is created using the data retrieved from the
     *      `ResultSet` `rs`.
     */
    @Override
    public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Notification(
            rs.getLong("id"),
            rs.getString("confirmation_id"),
            rs.getString("zip_file_name"),
            rs.getString("po_box"),
            rs.getString("container"),
            rs.getString("service"),
            rs.getString("document_control_number"),
            ErrorCode.valueOf(rs.getString("error_code")),
            rs.getString("error_description"),
            rs.getTimestamp("created_at").toInstant(),
            getOptionalInstant(rs.getTimestamp("processed_at")),
            NotificationStatus.valueOf(rs.getString("status")),
            rs.getString("message_id")
        );
    }

    /**
     * The function `getOptionalInstant` converts a `Timestamp` object to an `Instant` object, returning `null` if the
     * input is `null`.
     *
     * @param sqlTimestamp The `sqlTimestamp` parameter is of type `Timestamp` and represents a timestamp value in SQL.
     * @return An Instant object is being returned.
     */
    private Instant getOptionalInstant(Timestamp sqlTimestamp) {
        return sqlTimestamp == null ? null : sqlTimestamp.toInstant();
    }
}
