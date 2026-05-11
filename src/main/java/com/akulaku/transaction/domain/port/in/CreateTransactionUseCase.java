package com.akulaku.transaction.domain.port.in;

import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.model.TransactionStatus;
import com.akulaku.transaction.domain.model.TransactionType;

import java.time.Instant;
import java.util.UUID;

public interface CreateTransactionUseCase {

    CreateTransactionResult execute(CreateTransactionCommand command);

    record CreateTransactionCommand(
        String idempotencyKey,
        UUID userId,
        String externalRef,
        Money amount,
        Integer tenorMonths,
        TransactionType transactionType,
        String merchantId
    ) {

        public CreateTransactionCommand {
            if (idempotencyKey == null || idempotencyKey.isBlank()) {
                throw new IllegalArgumentException("idempotencyKey must not be blank");
            }
        }
    }

    record CreateTransactionResult(
        UUID transactionId,
        TransactionStatus status,
        Money remainingLimit,
        Instant createdAt,
        boolean replayedFromIdempotency
    ) {
    }
}
