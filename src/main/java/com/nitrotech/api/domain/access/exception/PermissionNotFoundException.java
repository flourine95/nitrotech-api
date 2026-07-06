package com.nitrotech.api.domain.access.exception;

import com.nitrotech.api.shared.exception.BadRequestException;

public class PermissionNotFoundException extends BadRequestException {

    public PermissionNotFoundException() {
        super("PERMISSION_NOT_FOUND", "One or more permissions were not found");
    }
}
