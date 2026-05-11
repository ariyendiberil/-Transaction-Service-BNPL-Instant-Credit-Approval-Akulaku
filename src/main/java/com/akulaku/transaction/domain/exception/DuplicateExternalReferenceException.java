package com.akulaku.transaction.domain.exception;

public final class DuplicateExternalReferenceException extends DomainException {

    public DuplicateExternalReferenceException(String externalRef) {
        super("A transaction with external reference already exists: " + externalRef);
    }
}
