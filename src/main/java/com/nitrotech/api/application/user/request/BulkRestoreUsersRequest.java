package com.nitrotech.api.application.user.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkRestoreUsersRequest(
        @NotEmpty(message = "Danh sách ID không được để trống")
        @Size(max = 100, message = "Không thể khôi phục quá 100 tài khoản một lúc")
        List<Long> ids
) {}
