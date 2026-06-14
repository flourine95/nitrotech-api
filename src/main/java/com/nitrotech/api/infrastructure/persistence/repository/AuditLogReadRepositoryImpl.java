package com.nitrotech.api.infrastructure.persistence.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nitrotech.api.domain.audit.dto.AuditLogEntryData;
import com.nitrotech.api.domain.audit.dto.AuditLogQuery;
import com.nitrotech.api.domain.audit.repository.AuditLogReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AuditLogReadRepositoryImpl implements AuditLogReadRepository {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {};

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Page<AuditLogEntryData> findAll(AuditLogQuery query) {
        List<Object> params = new ArrayList<>();
        String where = whereClause(query, params);
        int page = Math.max(query.page(), 0);
        int size = Math.min(Math.max(query.size(), 1), 100);

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM audit_logs " + where, Long.class, params.toArray());
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add((long) page * size);

        String sortColumn = "created_at";
        if ("id".equalsIgnoreCase(query.sortBy())) {
            sortColumn = "id";
        }
        String direction = "DESC";
        if ("asc".equalsIgnoreCase(query.sortDir())) {
            direction = "ASC";
        }

        List<AuditLogEntryData> content = jdbc.query("""
                SELECT id, correlation_id, actor_type, actor_id, actor_email, actor_roles::text,
                       action, resource_type, resource_id, outcome,
                       before_data::text, after_data::text, metadata::text,
                       ip_address, user_agent, created_at
                FROM audit_logs
                """ + (where.isEmpty() ? "" : " " + where) + " ORDER BY " + sortColumn + " " + direction + ", id DESC LIMIT ? OFFSET ?",
                this::mapRow, dataParams.toArray());

        return new PageImpl<>(content, PageRequest.of(page, size), total == null ? 0 : total);
    }

    private String whereClause(AuditLogQuery query, List<Object> params) {
        List<String> filters = new ArrayList<>();
        addEquals(filters, params, "action", query.action());
        addEquals(filters, params, "resource_type", query.resourceType());
        addEquals(filters, params, "outcome", query.outcome());
        addEquals(filters, params, "correlation_id", query.correlationId());
        addEquals(filters, params, "resource_id", query.resourceId());
        if (query.actor() != null && !query.actor().isBlank()) {
            filters.add("(CAST(actor_id AS TEXT) = ? OR LOWER(actor_email) LIKE LOWER(?))");
            params.add(query.actor().trim());
            params.add("%" + query.actor().trim() + "%");
        }
        return filters.isEmpty() ? "" : "WHERE " + String.join(" AND ", filters);
    }

    private void addEquals(List<String> filters, List<Object> params, String column, String value) {
        if (value != null && !value.isBlank()) {
            filters.add(column + " = ?");
            params.add(value.trim());
        }
    }

    private AuditLogEntryData mapRow(ResultSet rs, int rowNum) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new AuditLogEntryData(
                rs.getLong("id"),
                rs.getString("correlation_id"),
                rs.getString("actor_type"),
                nullableLong(rs, "actor_id"),
                rs.getString("actor_email"),
                readList(rs.getString("actor_roles")),
                rs.getString("action"),
                rs.getString("resource_type"),
                rs.getString("resource_id"),
                rs.getString("outcome"),
                readMap(rs.getString("before_data")),
                readMap(rs.getString("after_data")),
                readMap(rs.getString("metadata")),
                rs.getString("ip_address"),
                rs.getString("user_agent"),
                createdAt == null ? null : createdAt.toInstant()
        );
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private List<String> readList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, OBJECT_MAP);
        } catch (Exception ignored) {
            return null;
        }
    }
}
