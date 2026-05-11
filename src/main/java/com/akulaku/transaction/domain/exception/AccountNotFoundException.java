package com.akulaku.transaction.domain.exception;

public final class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(java.util.UUID userId) {
        super("Credit account not found for user: " + userId);
    }
}
