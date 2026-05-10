package com.nitrotech.api.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(description = "Standard API response wrapper")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResult<T>(
        @Schema(description = "Response payload")
        T data,

        @Schema(description = "Human-readable message", example = "Created successfully")
        String message,

        @Schema(description = "Pagination metadata (present on paged responses)", nullable = true)
        PageMeta meta,

        @Schema(description = "Additional statistics or metadata (endpoint-specific)", nullable = true)
        Object facets
) {
    public static <T> ApiResult<T> ok(T data) {
        return new ApiResult<>(data, null, null, null);
    }

    public static <T> ApiResult<T> ok(T data, String message) {
        return new ApiResult<>(data, message, null, null);
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

    /** @deprecated dùng paged(Page<T>) thay thế */
    @Deprecated
    public static <T> ApiResult<List<T>> paginated(List<T> data, long page, long size, long total) {
        PageMeta meta = new PageMeta((int) page, (int) size, total,
                (int) Math.ceil((double) total / size), page < Math.ceil((double) total / size) - 1, page > 0);
        return new ApiResult<>(data, null, meta, null);
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
