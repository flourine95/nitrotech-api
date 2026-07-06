package com.nitrotech.api.infrastructure.persistence.repository;

import com.nitrotech.api.domain.auth.UserStatus;
import com.nitrotech.api.domain.user.dto.AdminUserData;
import com.nitrotech.api.domain.user.dto.AdminUserFacets;
import com.nitrotech.api.domain.user.dto.AdminUserFilter;
import com.nitrotech.api.domain.user.repository.AdminUserRepository;
import com.nitrotech.api.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class AdminUserRepositoryImpl implements AdminUserRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "name"),
            Map.entry("email", "email"),
            Map.entry("status", "status"),
            Map.entry("provider", "provider"),
            Map.entry("orderCount", "order_count"),
            Map.entry("totalSpent", "total_spent"),
            Map.entry("averageOrderValue", "average_order_value"),
            Map.entry("lastOrderAt", "last_order_at"),
            Map.entry("createdAt", "created_at"),
            Map.entry("updatedAt", "updated_at")
    );

    private final NamedParameterJdbcTemplate jdbc;
    private final UserJpaRepository jpa;

    @Override
    public Page<AdminUserData> findAll(AdminUserFilter filter, Pageable pageable) {
        MapSqlParameterSource params = params(filter)
                .addValue("limit", pageable.getPageSize())
                .addValue("offset", pageable.getOffset());
        String baseSql = baseSql() + whereSql(filter);
        long total = count(baseSql, params);
        if (total == 0) {
            return Page.empty(pageable);
        }

        String sql = """
                SELECT *
                FROM (
                """ + baseSql + """
                ) users_view
                ORDER BY\s""" + orderBy(pageable.getSort()) + "\nLIMIT :limit OFFSET :offset";
        List<AdminUserData> users = jdbc.query(sql, params, (rs, rowNum) -> toData(rs));
        return new PageImpl<>(users, pageable, total);
    }

    @Override
    public Optional<AdminUserData> findById(Long id) {
        MapSqlParameterSource params = new MapSqlParameterSource("id", id);
        List<AdminUserData> users = jdbc.query("""
                SELECT *
                FROM (
                """ + baseSql() + """
                WHERE u.id = :id AND u.deleted_at IS NULL
                ) users_view
                """, params, (rs, rowNum) -> toData(rs));
        return users.stream().findFirst();
    }

    @Override
    @Transactional
    public AdminUserData create(String name, String email, String phone, String status, String hashedPassword, Set<String> roleSlugs) {
        UserEntity entity = new UserEntity();
        entity.setName(name);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setPassword(hashedPassword);
        entity.setStatus(UserStatus.fromValue(status == null ? "active" : status));
        UserEntity saved = jpa.save(entity);
        replaceRoles(saved.getId(), roleSlugs == null || roleSlugs.isEmpty() ? Set.of("customer") : roleSlugs);
        return findById(saved.getId()).orElseThrow();
    }

    @Override
    @Transactional
    public AdminUserData update(Long id, String name, String email, String phone, String status) {
        UserEntity entity = jpa.findAllNotDeletedByIds(List.of(id)).stream()
                .findFirst()
                .orElseThrow();
        if (name != null) entity.setName(name);
        if (email != null) entity.setEmail(email);
        if (phone != null) entity.setPhone(phone);
        if (status != null) entity.setStatus(UserStatus.fromValue(status));
        jpa.save(entity);
        return findById(id).orElseThrow();
    }

    @Override
    public AdminUserFacets countFacets(AdminUserFilter filter) {
        MapSqlParameterSource params = params(filter);
        List<AdminUserFacets> rows = jdbc.query("""
                SELECT
                    COUNT(*) AS total,
                    COUNT(*) FILTER (WHERE status = 'active') AS active,
                    COUNT(*) FILTER (WHERE status = 'inactive') AS inactive,
                    COUNT(*) FILTER (WHERE created_at >= NOW() - INTERVAL '30 days') AS new_users,
                    COUNT(*) FILTER (WHERE order_count > 0) AS with_orders,
                    COUNT(*) FILTER (WHERE order_count = 0) AS no_orders,
                    COUNT(*) FILTER (
                        WHERE order_count > 0
                          AND last_order_at < NOW() - INTERVAL '90 days'
                    ) AS at_risk,
                    COALESCE(SUM(total_spent), 0) AS total_spent
                FROM (
                """ + baseSql() + whereSql(filter) + """
                ) users_view
                """, params, (rs, rowNum) -> new AdminUserFacets(
                rs.getLong("total"),
                rs.getLong("active"),
                rs.getLong("inactive"),
                rs.getLong("new_users"),
                rs.getLong("with_orders"),
                rs.getLong("no_orders"),
                rs.getLong("at_risk"),
                rs.getBigDecimal("total_spent")
        ));
        return rows.isEmpty()
                ? new AdminUserFacets(0, 0, 0, 0, 0, 0, 0, BigDecimal.ZERO)
                : rows.getFirst();
    }

    private String baseSql() {
        return """
                WITH order_stats AS (
                    SELECT
                        user_id,
                        COUNT(*) AS order_count,
                        COALESCE(SUM(final_amount), 0) AS total_spent,
                        COALESCE(AVG(final_amount), 0) AS average_order_value,
                        MAX(created_at) AS last_order_at
                    FROM orders
                    WHERE deleted_at IS NULL
                    GROUP BY user_id
                ),
                role_stats AS (
                    SELECT ur.user_id, ARRAY_AGG(r.slug ORDER BY r.slug) AS role_slugs
                    FROM user_roles ur
                    JOIN roles r ON r.id = ur.role_id
                    WHERE r.deleted_at IS NULL
                    GROUP BY ur.user_id
                )
                SELECT
                    u.id,
                    u.name,
                    u.email,
                    u.phone,
                    u.avatar,
                    u.status,
                    u.provider,
                    COALESCE(rs.role_slugs, ARRAY[]::varchar[]) AS role_slugs,
                    CASE
                        WHEN COALESCE(os.order_count, 0) > 0
                             AND os.last_order_at < NOW() - INTERVAL '90 days' THEN 'at_risk'
                        WHEN u.created_at >= NOW() - INTERVAL '30 days' THEN 'new'
                        WHEN COALESCE(os.order_count, 0) = 0 THEN 'no_orders'
                        ELSE 'with_orders'
                    END AS customer_state,
                    COALESCE(os.order_count, 0) AS order_count,
                    COALESCE(os.total_spent, 0) AS total_spent,
                    COALESCE(os.average_order_value, 0) AS average_order_value,
                    os.last_order_at,
                    u.created_at,
                    u.updated_at
                FROM users u
                LEFT JOIN order_stats os ON os.user_id = u.id
                LEFT JOIN role_stats rs ON rs.user_id = u.id
                """;
    }

    private String whereSql(AdminUserFilter filter) {
        StringBuilder sql = new StringBuilder();
        if (Boolean.TRUE.equals(filter.deleted())) {
            sql.append(" WHERE u.deleted_at IS NOT NULL");
        } else {
            sql.append(" WHERE u.deleted_at IS NULL");
        }
        if (filter.search() != null) {
            sql.append("""
                     AND (
                        LOWER(u.name) LIKE :search
                        OR LOWER(u.email) LIKE :search
                        OR LOWER(COALESCE(u.phone, '')) LIKE :search
                     )
                    """);
        }
        if (filter.status() != null) {
            sql.append(" AND u.status = :status");
        }
        if (filter.provider() != null) {
            sql.append(" AND u.provider = :provider");
        }
        if (filter.role() != null) {
            sql.append("""
                     AND EXISTS (
                        SELECT 1
                        FROM user_roles ur
                        JOIN roles r ON r.id = ur.role_id
                        WHERE ur.user_id = u.id
                          AND r.slug = :role
                          AND r.deleted_at IS NULL
                     )
                    """);
        }
        if (filter.createdFrom() != null) {
            sql.append(" AND u.created_at >= :createdFrom");
        }
        if (filter.createdToExclusive() != null) {
            sql.append(" AND u.created_at < :createdTo");
        }
        if (filter.activity() != null) {
            sql.append(switch (filter.activity()) {
                case "new" -> " AND u.created_at >= NOW() - INTERVAL '30 days'";
                case "with_orders" -> " AND COALESCE(os.order_count, 0) > 0";
                case "no_orders" -> " AND COALESCE(os.order_count, 0) = 0";
                case "at_risk" -> " AND COALESCE(os.order_count, 0) > 0 AND os.last_order_at < NOW() - INTERVAL '90 days'";
                default -> "";
            });
        }
        return sql.toString();
    }

    private MapSqlParameterSource params(AdminUserFilter filter) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (filter.search() != null) {
            params.addValue("search", "%" + filter.search().toLowerCase() + "%");
        }
        params.addValue("status", filter.status());
        params.addValue("provider", filter.provider());
        params.addValue("role", filter.role());
        params.addValue("createdFrom", timestamp(filter.createdFrom()));
        params.addValue("createdTo", timestamp(filter.createdToExclusive()));
        return params;
    }

    private Timestamp timestamp(Instant instant) {
        return instant == null ? null : Timestamp.from(instant);
    }

    private long count(String baseSql, MapSqlParameterSource params) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM (" + baseSql + ") users_view", params, Long.class);
        return count == null ? 0 : count;
    }

    private String orderBy(Sort sort) {
        if (sort.isUnsorted()) {
            return "created_at DESC, id DESC";
        }
        StringBuilder order = new StringBuilder();
        for (Sort.Order item : sort) {
            String column = SORT_COLUMNS.get(item.getProperty());
            if (column == null) {
                continue;
            }
            if (!order.isEmpty()) {
                order.append(", ");
            }
            order.append(column).append(item.isAscending() ? " ASC" : " DESC");
        }
        return order.isEmpty() ? "created_at DESC, id DESC" : order.append(", id DESC").toString();
    }

    private AdminUserData toData(ResultSet rs) throws SQLException {
        return new AdminUserData(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("avatar"),
                rs.getString("status"),
                rs.getString("provider"),
                roleSlugs(rs.getArray("role_slugs")),
                rs.getString("customer_state"),
                rs.getLong("order_count"),
                rs.getBigDecimal("total_spent"),
                rs.getBigDecimal("average_order_value"),
                instant(rs, "last_order_at"),
                instant(rs, "created_at"),
                instant(rs, "updated_at")
        );
    }

    private Set<String> roleSlugs(Array array) throws SQLException {
        if (array == null) {
            return Set.of();
        }
        Object raw = array.getArray();
        if (raw instanceof String[] slugs) {
            return new LinkedHashSet<>(List.of(slugs));
        }
        if (raw instanceof Object[] slugs) {
            Set<String> values = new LinkedHashSet<>();
            for (Object slug : slugs) {
                if (slug != null) {
                    values.add(slug.toString());
                }
            }
            return values;
        }
        return Set.of();
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        var timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }

    @Override
    @Transactional
    public List<Long> bulkSoftDelete(List<Long> ids) {
        List<Long> deletableIds = jpa.findAllNotDeletedByIds(ids).stream()
                .map(UserEntity::getId).toList();
        if (!deletableIds.isEmpty()) {
            jpa.bulkSoftDelete(deletableIds, Instant.now());
        }
        return deletableIds;
    }

    @Override
    @Transactional
    public List<Long> bulkUpdateStatus(List<Long> ids, String status) {
        UserStatus enumStatus;
        try {
            enumStatus = UserStatus.fromValue(status);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        List<Long> notDeletedIds = jpa.findAllNotDeletedByIds(ids).stream()
                .map(UserEntity::getId).toList();
        if (!notDeletedIds.isEmpty()) {
            jpa.bulkUpdateStatus(notDeletedIds, enumStatus);
        }
        return notDeletedIds;
    }

    @Override
    public Map<Long, String> findStatusesByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return jpa.findAllNotDeletedByIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(
                        UserEntity::getId,
                        entity -> entity.getStatus().name()
                ));
    }

    @Override
    @Transactional
    public List<Long> bulkRestore(List<Long> ids) {
        List<Long> deletedIds = jpa.findAllDeletedByIds(ids).stream()
                .map(UserEntity::getId).toList();
        if (!deletedIds.isEmpty()) {
            jpa.bulkRestore(deletedIds);
        }
        return deletedIds;
    }

    @Override
    public List<String> findEmailsByIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return jpa.findAllById(ids).stream()
                .map(UserEntity::getEmail)
                .toList();
    }

    private void replaceRoles(Long userId, Set<String> roleSlugs) {
        jdbc.update("DELETE FROM user_roles WHERE user_id = :userId", new MapSqlParameterSource("userId", userId));
        for (String roleSlug : roleSlugs) {
            jdbc.update("""
                    INSERT INTO user_roles (user_id, role_id)
                    SELECT :userId, r.id
                    FROM roles r
                    WHERE r.slug = :roleSlug
                      AND r.active = TRUE
                      AND r.deleted_at IS NULL
                    ON CONFLICT DO NOTHING
                    """, new MapSqlParameterSource()
                    .addValue("userId", userId)
                    .addValue("roleSlug", roleSlug));
        }
    }
}
