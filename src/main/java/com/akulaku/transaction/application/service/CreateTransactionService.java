package com.akulaku.transaction.application.service;

import com.akulaku.transaction.application.support.IdempotencyHasher;
import com.akulaku.transaction.domain.exception.IdempotencyConflictException;
import com.akulaku.transaction.domain.exception.IdempotencyInProgressException;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase;
import com.akulaku.transaction.domain.port.out.CreateTransactionResultSerializer;
import com.akulaku.transaction.domain.port.out.IdempotencyStore;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
public class CreateTransactionService implements CreateTransactionUseCase {

    private static final String ENDPOINT = "POST /api/v1/transactions";
    private static final int HTTP_CREATED = 201;

    private final IdempotencyStore idempotencyStore;
    private final IdempotencyHasher idempotencyHasher;
    private final CreateTransactionResultSerializer resultSerializer;
    private final CreateTransactionWorkflow workflow;
    private final Clock clock;

    public CreateTransactionService(
        IdempotencyStore idempotencyStore,
        IdempotencyHasher idempotencyHasher,
        CreateTransactionResultSerializer resultSerializer,
        CreateTransactionWorkflow workflow,
        Clock clock
    ) {
        this.idempotencyStore = idempotencyStore;
        this.idempotencyHasher = idempotencyHasher;
        this.resultSerializer = resultSerializer;
        this.workflow = workflow;
        this.clock = clock;
    }

    @Override
    public CreateTransactionResult execute(CreateTransactionCommand command) {
        String requestHash = idempotencyHasher.hash(command);
        IdempotencyStore.IdempotencyReservation reservation =
            idempotencyStore.reserve(command.idempotencyKey(), requestHash, ENDPOINT);

        return switch (reservation.status()) {
            case ALREADY_COMPLETED -> replayCached(reservation.cachedResponseBody());
            case CONFLICT -> throw new IdempotencyConflictException(command.idempotencyKey());
            case ALREADY_PROCESSING -> throw new IdempotencyInProgressException(command.idempotencyKey());
            case NEW -> executeFresh(command);
        };
    }

    private CreateTransactionResult replayCached(String cachedResponseBody) {
        CreateTransactionResult cached = resultSerializer.fromJson(cachedResponseBody);
        return new CreateTransactionResult(
            cached.transactionId(),
            cached.status(),
            cached.remainingLimit(),
            cached.createdAt(),
            true
        );
    }

    private CreateTransactionResult executeFresh(CreateTransactionCommand command) {
        Instant now = clock.instant();
        try {
            CreateTransactionResult result = workflow.execute(command, now);
            String body = resultSerializer.toJson(result);
            idempotencyStore.complete(command.idempotencyKey(), HTTP_CREATED, body);
            return result;
        } catch (RuntimeException ex) {
            idempotencyStore.markFailed(command.idempotencyKey());
            throw ex;
        }
    }
}
