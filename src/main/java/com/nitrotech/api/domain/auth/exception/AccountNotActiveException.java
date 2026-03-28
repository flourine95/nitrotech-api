package com.nitrotech.api.domain.auth.exception;

import com.nitrotech.api.shared.exception.DomainException;

public class AccountNotActiveException extends DomainException {

    public AccountNotActiveException(String status) {
        super("ACCOUNT_NOT_ACTIVE", "Account is " + status + " and cannot login");
    }
}
