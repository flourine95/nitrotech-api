package com.nitrotech.api.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
        T data,

        String message,

        PageMeta meta,

        Object facets
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(data, null, null, null);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(data, message, null, null);
    }

    public static ApiResult<Void> ok(String message) {
        return new ApiResult<>(null, message, null, null);
    }

    public static <T> ApiResult<T> created(T data) {
        return new ApiResult<>(data, "Created successfully", null, null);
    }

    public static <T> ApiResult<List<T>> paged(Page<T> page) {
        return new ApiResult<>(page.getContent(), null, PageMeta.from(page), null);
    }

    public static <T> ApiResult<List<T>> paged(Page<T> page, Object facets) {
        return new ApiResult<>(page.getContent(), null, PageMeta.from(page), facets);
    }

    public record PageMeta(
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
        public static PageMeta from(Page<?> page) {
            return new PageMeta(
                    page.getNumber(),
                    page.getSize(),
                    page.getTotalElements(),
                    page.getTotalPages(),
                    page.hasNext(),
                    page.hasPrevious()
            );
        }
    }
}
