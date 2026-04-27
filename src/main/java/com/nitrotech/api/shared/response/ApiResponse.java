package com.nitrotech.api.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.data.domain.Page;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        String message,
        PageMeta meta,
        Object facets
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message, null, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(data, "Created successfully", null, null);
    }

    public static <T> ApiResponse<List<T>> paged(Page<T> page) {
        return new ApiResponse<>(page.getContent(), null, PageMeta.from(page), null);
    }

    public static <T> ApiResponse<List<T>> paged(Page<T> page, Object facets) {
        return new ApiResponse<>(page.getContent(), null, PageMeta.from(page), facets);
    }

    /** @deprecated dùng paged(Page<T>) thay thế */
    @Deprecated
    public static <T> ApiResponse<List<T>> paginated(List<T> data, long page, long size, long total) {
        PageMeta meta = new PageMeta((int) page, (int) size, total,
                (int) Math.ceil((double) total / size), page < Math.ceil((double) total / size) - 1, page > 0);
        return new ApiResponse<>(data, null, meta, null);
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
