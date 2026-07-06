package com.nitrotech.api.domain.user.exception;

import com.nitrotech.api.shared.exception.ForbiddenException;

public class SelfStatusChangeException extends ForbiddenException {

    public SelfStatusChangeException() {
        super("SELF_STATUS_CHANGE", "Không thể tự khóa tài khoản của chính mình");
    }
}
