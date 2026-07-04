package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.notification.dto.NotificationData;
import com.nitrotech.api.domain.notification.dto.NotificationEvent;
import com.nitrotech.api.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryImpl implements NotificationRepository {

    private final JdbcTemplate jdbc;

    @Override
    public NotificationData save(NotificationEvent event) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        Long recipientUserId = parseLong(event.recipientUserId());
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO notifications (type, title, message, href, recipient_user_id, required_authority)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            ps.setString(1, event.type());
            ps.setString(2, event.title());
            ps.setString(3, event.message());
            ps.setString(4, event.href());
            if (recipientUserId == null) {
                ps.setObject(5, null);
            } else {
                ps.setLong(5, recipientUserId);
            }
            ps.setString(6, event.requiredAuthority());
            return ps;
        }, keyHolder);

        Number id = Objects.requireNonNull(keyHolder.getKey(), "notification id");
        return findById(id.longValue());
    }

    @Override
    public List<NotificationData> findVisibleForUser(Long userId, Set<String> authorities, int size) {
        int limit = Math.min(Math.max(size, 1), 100);
        return jdbc.query("""
                SELECT n.id, n.type, n.title, n.message, n.href, n.recipient_user_id,
                       n.required_authority, n.created_at, nr.read_at IS NOT NULL AS read
                FROM notifications n
                LEFT JOIN notification_reads nr ON nr.notification_id = n.id AND nr.user_id = ?
                WHERE n.recipient_user_id = ? OR n.required_authority = ANY (?)
                ORDER BY n.created_at DESC, n.id DESC
                LIMIT ?
                """, this::mapRow, userId, userId, authorities.toArray(String[]::new), limit);
    }

    @Override
    public boolean markAsRead(Long notificationId, Long userId, Set<String> authorities) {
        List<Integer> inserted = jdbc.query("""
                WITH visible AS (
                    SELECT id
                    FROM notifications
                    WHERE id = ? AND (recipient_user_id = ? OR required_authority = ANY (?))
                )
                INSERT INTO notification_reads (notification_id, user_id)
                SELECT id, ? FROM visible
                ON CONFLICT (notification_id, user_id) DO NOTHING
                RETURNING 1
                """, (rs, rowNum) -> rs.getInt(1), notificationId, userId, authorities.toArray(String[]::new), userId);
        return !inserted.isEmpty();
    }

    @Override
    public void markAllAsRead(Long userId, Set<String> authorities) {
        jdbc.update("""
                INSERT INTO notification_reads (notification_id, user_id)
                SELECT id, ?
                FROM notifications
                WHERE recipient_user_id = ? OR required_authority = ANY (?)
                ON CONFLICT (notification_id, user_id) DO NOTHING
                """, userId, userId, authorities.toArray(String[]::new));
    }

    private NotificationData findById(Long id) {
        return jdbc.queryForObject("""
                SELECT id, type, title, message, href, recipient_user_id,
                       required_authority, created_at, false AS read
                FROM notifications
                WHERE id = ?
                """, this::mapRow, id);
    }

    private NotificationData mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new NotificationData(
                rs.getLong("id"),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("message"),
                rs.getString("href"),
                nullableLong(rs, "recipient_user_id"),
                rs.getString("required_authority"),
                createdAt == null ? null : createdAt.toInstant(),
                rs.getBoolean("read")
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Long.parseLong(value);
    }
}
