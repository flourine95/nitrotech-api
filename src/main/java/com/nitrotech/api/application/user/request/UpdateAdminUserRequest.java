package com.nitrotech.api.application.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAdminUserRequest(
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Pattern(regexp = "active|inactive|suspended|banned", message = "Status is invalid")
        String status
) {}
