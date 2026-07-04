package com.nitrotech.api.application.user.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CreateAdminUserRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email is invalid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Pattern(regexp = "active|inactive|suspended|banned", message = "Status is invalid")
        String status,

        Set<String> roleSlugs
) {}
