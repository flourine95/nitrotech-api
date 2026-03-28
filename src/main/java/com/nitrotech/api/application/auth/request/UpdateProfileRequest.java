package com.nitrotech.api.application.auth.request;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
        String name,

        @Size(max = 20, message = "Phone must be at most 20 characters")
        String phone,

        String avatar
) {}
