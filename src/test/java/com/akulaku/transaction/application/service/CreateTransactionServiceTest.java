package com.akulaku.transaction.application.service;

import com.akulaku.transaction.application.support.IdempotencyHasher;
import com.akulaku.transaction.domain.model.Currency;
import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.model.TransactionStatus;
import com.akulaku.transaction.domain.model.TransactionType;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;
import com.akulaku.transaction.domain.port.out.CreateTransactionResultSerializer;
import com.akulaku.transaction.domain.port.out.IdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTransactionServiceTest {

    @Mock
    private IdempotencyStore idempotencyStore;

    @Mock
    private IdempotencyHasher idempotencyHasher;

    @Mock
    private CreateTransactionResultSerializer resultSerializer;

    @Mock
    private CreateTransactionWorkflow workflow;

    private CreateTransactionService service;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-11T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        service = new CreateTransactionService(
            idempotencyStore,
            idempotencyHasher,
            resultSerializer,
            workflow,
            clock
        );
    }

    @Test
    void execute_replaysCachedResponse_whenIdempotencyAlreadyCompleted() {
        UUID txId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        CreateTransactionCommand command = new CreateTransactionCommand(
            "idem-replay",
            UUID.randomUUID(),
            "ORDER-99",
            Money.of(new BigDecimal("1000"), Currency.IDR),
            3,
            TransactionType.PURCHASE,
            "MERCH-1"
        );

        when(idempotencyHasher.hash(command)).thenReturn("hash-1");
        when(idempotencyStore.reserve(eq("idem-replay"), eq("hash-1"), anyString()))
            .thenReturn(new IdempotencyStore.IdempotencyReservation(
                IdempotencyStore.ReservationStatus.ALREADY_COMPLETED,
                201,
                "{\"payload\":true}"
            ));

        CreateTransactionResult stored = new CreateTransactionResult(
            txId,
            TransactionStatus.APPROVED,
            Money.of(new BigDecimal("9000000"), Currency.IDR),
            Instant.parse("2026-05-10T12:00:00Z"),
            false
        );
        when(resultSerializer.fromJson("{\"payload\":true}")).thenReturn(stored);

        CreateTransactionResult result = service.execute(command);

        assertThat(result.transactionId()).isEqualTo(txId);
        assertThat(result.replayedFromIdempotency()).isTrue();
        verify(workflow, never()).execute(any(), any());
        verify(idempotencyStore, never()).complete(anyString(), anyInt(), anyString());
    }
}
