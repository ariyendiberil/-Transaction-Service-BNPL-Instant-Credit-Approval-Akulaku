package com.akulaku.transaction.domain.exception;

public final class IdempotencyInProgressException extends DomainException {

    public IdempotencyInProgressException(String idempotencyKey) {
        super("Request with this idempotency key is still being processed: " + idempotencyKey);
    }
}
