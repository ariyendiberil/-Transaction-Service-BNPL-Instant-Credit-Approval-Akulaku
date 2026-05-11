package com.akulaku.transaction.domain.exception;

/**
 * Base for domain-layer failures (business rule violations).
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
