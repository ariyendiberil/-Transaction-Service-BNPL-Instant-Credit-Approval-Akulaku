package com.akulaku.transaction.infrastructure.serialization;

import com.akulaku.transaction.domain.model.Currency;
import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.model.TransactionStatus;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;
import com.akulaku.transaction.domain.port.out.CreateTransactionResultSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Component
public class JacksonCreateTransactionResultSerializer implements CreateTransactionResultSerializer {

    private final ObjectMapper objectMapper;

    public JacksonCreateTransactionResultSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String toJson(CreateTransactionResult result) {
        try {
            return objectMapper.writeValueAsString(Wire.from(result));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize CreateTransactionResult", e);
        }
    }

    @Override
    public CreateTransactionResult fromJson(String json) {
        try {
            Wire wire = objectMapper.readValue(json, Wire.class);
            return wire.toResult();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize CreateTransactionResult", e);
        }
    }

    private record Wire(
        UUID transactionId,
        TransactionStatus status,
        BigDecimal remainingAmount,
        String remainingCurrency,
        Instant createdAt,
        boolean replayedFromIdempotency
    ) {
        static Wire from(CreateTransactionResult r) {
            return new Wire(
                r.transactionId(),
                r.status(),
                r.remainingLimit().amount(),
                r.remainingLimit().currency().name(),
                r.createdAt(),
                r.replayedFromIdempotency()
            );
        }

        CreateTransactionResult toResult() {
            return new CreateTransactionResult(
                transactionId,
                status,
                Money.of(remainingAmount, Currency.valueOf(remainingCurrency)),
                createdAt,
                replayedFromIdempotency
            );
        }
    }
}
