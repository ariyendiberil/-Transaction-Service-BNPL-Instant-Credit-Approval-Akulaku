package com.akulaku.transaction.domain.event;

import java.math.BigDecimal;
import java.util.UUID;

public record TransactionCreatedEvent(
    UUID transactionId,
    UUID accountId,
    UUID userId,
    String externalRef,
    BigDecimal amount,
    String currencyCode
) {
}
