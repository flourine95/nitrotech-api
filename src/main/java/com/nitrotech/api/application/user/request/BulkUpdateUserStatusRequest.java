package com.nitrotech.api.application.user.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkUpdateUserStatusRequest(
        @NotEmpty(message = "Danh sách ID không được để trống")
        @Size(max = 100, message = "Không thể cập nhật quá 100 tài khoản một lúc")
        List<Long> ids,

        @NotBlank(message = "Trạng thái không được để trống")
        @Pattern(regexp = "active|inactive|suspended|banned", message = "Trạng thái không hợp lệ")
        String status
) {}
