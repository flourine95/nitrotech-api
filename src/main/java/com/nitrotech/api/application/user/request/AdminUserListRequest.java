package com.nitrotech.api.application.user.request;

import com.nitrotech.api.domain.user.dto.AdminUserFilter;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AdminUserListRequest {

    @Size(max = 100, message = "Search query must not exceed 100 characters")
    private String search;

    private String status;
    private String provider;
    private String role;
    private String activity;
    private String createdFrom;
    private String createdTo;
    private Boolean deleted;

    public AdminUserFilter toFilter() {
        return new AdminUserFilter(
                blankToNull(search),
                blankToNull(status),
                blankToNull(provider),
                blankToNull(role),
                blankToNull(activity),
                parseTimestamp(createdFrom),
                parseTimestamp(createdTo),
                deleted
        );
    }

    private Instant parseTimestamp(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : Instant.parse(normalized);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
