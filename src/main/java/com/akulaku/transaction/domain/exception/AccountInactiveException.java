package com.akulaku.transaction.domain.exception;

import com.akulaku.transaction.domain.model.AccountStatus;

public final class AccountInactiveException extends DomainException {

    private final AccountStatus status;

    public AccountInactiveException(AccountStatus status) {
        super("Credit account is not active: " + status);
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }
}
