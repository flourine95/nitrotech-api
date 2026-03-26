package com.nitrotech.api.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        T data,
        String message,
        Meta meta
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(data, message, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(data, "Created successfully", null);
    }

    public static <T> ApiResponse<T> paginated(T data, long page, long size, long total) {
        return new ApiResponse<>(data, null, new Meta(page, size, total));
    }

    public record Meta(long page, long size, long total) {}
}
