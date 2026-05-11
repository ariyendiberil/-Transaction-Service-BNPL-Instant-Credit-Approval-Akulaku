package com.akulaku.transaction.domain.model;

import com.akulaku.transaction.domain.exception.AccountInactiveException;
import com.akulaku.transaction.domain.exception.InsufficientLimitException;

import java.util.Objects;
import java.util.UUID;

/**
 * Aggregate root representing a user's BNPL credit line and remaining available limit.
 */
public class CreditAccount {

    private final UUID id;
    private final UUID userId;
    private final String accountNumber;
    private final Money creditLimit;
    private Money availableLimit;
    private AccountStatus status;
    private long version;

    public CreditAccount(
        UUID id,
        UUID userId,
        String accountNumber,
        Money creditLimit,
        Money availableLimit,
        AccountStatus status,
        long version
    ) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(accountNumber, "accountNumber");
        Objects.requireNonNull(creditLimit, "creditLimit");
        Objects.requireNonNull(availableLimit, "availableLimit");
        Objects.requireNonNull(status, "status");
        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be blank");
        }
        creditLimit.assertSameCurrencyAs(availableLimit);
        if (availableLimit.isGreaterThan(creditLimit)) {
            throw new IllegalArgumentException("availableLimit cannot exceed creditLimit");
        }
        this.id = id;
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.creditLimit = creditLimit;
        this.availableLimit = availableLimit;
        this.status = status;
        this.version = version;
    }

    /**
     * Reduces {@link #availableLimit} by {@code amount} when business rules allow.
     */
    public void debit(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) {
            throw new IllegalArgumentException("debit amount must be positive");
        }
        if (status != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(status);
        }
        amount.assertSameCurrencyAs(availableLimit);
        if (amount.isGreaterThan(availableLimit)) {
            throw new InsufficientLimitException(availableLimit, amount);
        }
        this.availableLimit = this.availableLimit.subtract(amount);
    }

    /**
     * Restores {@link #availableLimit} (e.g. refund / repayment), without exceeding {@link #creditLimit}.
     */
    public void credit(Money amount) {
        Objects.requireNonNull(amount, "amount");
        if (amount.isZero()) {
            throw new IllegalArgumentException("credit amount must be positive");
        }
        if (status != AccountStatus.ACTIVE) {
            throw new AccountInactiveException(status);
        }
        amount.assertSameCurrencyAs(availableLimit);
        Money newAvailable = availableLimit.add(amount);
        if (newAvailable.isGreaterThan(creditLimit)) {
            throw new IllegalStateException(
                "Credit would exceed credit limit; max top-up is %s"
                    .formatted(creditLimit.subtract(availableLimit))
            );
        }
        this.availableLimit = newAvailable;
    }

    public UUID id() {
        return id;
    }

    public UUID userId() {
        return userId;
    }

    public String accountNumber() {
        return accountNumber;
    }

    public Money creditLimit() {
        return creditLimit;
    }

    public Money availableLimit() {
        return availableLimit;
    }

    public AccountStatus status() {
        return status;
    }

    public long version() {
        return version;
    }

    public void bumpVersion() {
        this.version++;
    }
}
