package com.akulaku.transaction.domain.exception;

import com.akulaku.transaction.domain.model.Money;

public final class InsufficientLimitException extends DomainException {

    private final Money available;
    private final Money requested;

    public InsufficientLimitException(Money available, Money requested) {
        super("Available limit %s is less than requested %s".formatted(available, requested));
        this.available = available;
        this.requested = requested;
    }

    public Money getAvailable() {
        return available;
    }

    public Money getRequested() {
        return requested;
    }
}
