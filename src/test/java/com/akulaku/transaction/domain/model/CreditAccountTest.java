package com.akulaku.transaction.domain.model;

import com.akulaku.transaction.domain.exception.AccountInactiveException;
import com.akulaku.transaction.domain.exception.InsufficientLimitException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CreditAccountTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void debit_reducesAvailableLimit_whenAmountIsWithinLimit() {
        CreditAccount account = activeAccount(
            new BigDecimal("10000000"),
            new BigDecimal("10000000")
        );

        account.debit(Money.of(new BigDecimal("1500000"), Currency.IDR));

        assertThat(account.availableLimit().amount())
            .isEqualByComparingTo(new BigDecimal("8500000"));
        assertThat(account.availableLimit().currency()).isEqualTo(Currency.IDR);
    }

    @Test
    void debit_throwsInsufficientLimit_whenAmountExceedsAvailable() {
        CreditAccount account = activeAccount(
            new BigDecimal("10000000"),
            new BigDecimal("500000")
        );

        assertThatThrownBy(() -> account.debit(Money.of(new BigDecimal("501000"), Currency.IDR)))
            .isInstanceOf(InsufficientLimitException.class)
            .hasMessageContaining("Available limit");

        assertThat(account.availableLimit().amount())
            .isEqualByComparingTo(new BigDecimal("500000"));
    }

    @Test
    void debit_throwsAccountInactive_whenStatusNotActive() {
        CreditAccount suspended = new CreditAccount(
            ACCOUNT_ID,
            USER_ID,
            "ACC-001",
            Money.of(new BigDecimal("10000000"), Currency.IDR),
            Money.of(new BigDecimal("10000000"), Currency.IDR),
            AccountStatus.SUSPENDED,
            0L
        );

        assertThatThrownBy(() -> suspended.debit(Money.of(BigDecimal.ONE, Currency.IDR)))
            .isInstanceOf(AccountInactiveException.class)
            .hasMessageContaining("not active");

        assertThat(suspended.availableLimit().amount())
            .isEqualByComparingTo(new BigDecimal("10000000"));
    }

    private static CreditAccount activeAccount(BigDecimal creditLimit, BigDecimal available) {
        return new CreditAccount(
            ACCOUNT_ID,
            USER_ID,
            "ACC-001",
            Money.of(creditLimit, Currency.IDR),
            Money.of(available, Currency.IDR),
            AccountStatus.ACTIVE,
            0L
        );
    }
}
