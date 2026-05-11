package com.akulaku.transaction.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransactionResponse(
    UUID transactionId,
    String status,
    BigDecimal remainingLimit,
    String currency,
    Instant createdAt,
    boolean idempotencyReplayed
) {
}
