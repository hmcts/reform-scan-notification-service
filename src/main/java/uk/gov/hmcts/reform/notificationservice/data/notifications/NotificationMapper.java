package uk.gov.hmcts.reform.notificationservice.data.notifications;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class NotificationMapper implements RowMapper<Notification> {

    @Override
    public Notification mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Notification(
            rs.getLong("id"),
            rs.getString("zip_file_name"),
            rs.getString("po_box"),
            rs.getString("document_control_number"),
            rs.getString("error_code"),
            rs.getString("error_description"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getTimestamp("processed_at").toInstant(),
            rs.getString("service"),
            Status.valueOf(rs.getString("status"))
        );
    }
}
