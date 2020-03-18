package uk.gov.hmcts.reform.notificationservice.data;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.notificationservice.model.common.ErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

@Component
public class NotificationMapper implements RowMapper<Notification> {

    @Override
    public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Notification(
            rs.getLong("id"),
            rs.getString("notification_id"),
            rs.getString("zip_file_name"),
            rs.getString("po_box"),
            rs.getString("service"),
            rs.getString("document_control_number"),
            ErrorCode.valueOf(rs.getString("error_code")),
            rs.getString("error_description"),
            rs.getTimestamp("created_at").toInstant(),
            getOptionalInstant(rs.getTimestamp("processed_at")),
            NotificationStatus.valueOf(rs.getString("status"))
        );
    }

    private Instant getOptionalInstant(Timestamp sqlTimestamp) {
        return sqlTimestamp == null ? null : sqlTimestamp.toInstant();
    }
}
