package com.akulaku.transaction.domain.exception;

public final class IdempotencyConflictException extends DomainException {

    public IdempotencyConflictException(String idempotencyKey) {
        super("Idempotency key already used with a different request: " + idempotencyKey);
    }
}
