package com.akulaku.transaction.application.service;

import com.akulaku.transaction.domain.event.TransactionCreatedEvent;
import com.akulaku.transaction.domain.exception.InsufficientLimitException;
import com.akulaku.transaction.domain.model.AccountStatus;
import com.akulaku.transaction.domain.model.CreditAccount;
import com.akulaku.transaction.domain.model.Currency;
import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.model.TransactionStatus;
import com.akulaku.transaction.domain.model.TransactionType;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;
import com.akulaku.transaction.domain.port.out.CreditAccountRepository;
import com.akulaku.transaction.domain.port.out.LedgerRepository;
import com.akulaku.transaction.domain.port.out.OutboxAppender;
import com.akulaku.transaction.domain.port.out.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCreateTransactionWorkflowTest {

    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private CreditAccountRepository creditAccountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private OutboxAppender outboxAppender;

    private ObjectMapper objectMapper;

    private DefaultCreateTransactionWorkflow workflow;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        workflow = new DefaultCreateTransactionWorkflow(
            creditAccountRepository,
            transactionRepository,
            ledgerRepository,
            outboxAppender,
            objectMapper
        );
    }

    @Test
    void execute_persistsTransactionLedgerAndOutbox_whenDebitSucceeds() throws Exception {
        CreditAccount account = new CreditAccount(
            ACCOUNT_ID,
            USER_ID,
            "ACC-001",
            Money.of(new BigDecimal("10000000"), Currency.IDR),
            Money.of(new BigDecimal("10000000"), Currency.IDR),
            AccountStatus.ACTIVE,
            0L
        );
        when(creditAccountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByExternalRef("ORDER-55")).thenReturn(false);
        when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(ledgerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Instant now = Instant.parse("2026-05-10T10:15:00Z");
        CreateTransactionCommand command = new CreateTransactionCommand(
            "idem-1",
            USER_ID,
            "ORDER-55",
            Money.of(new BigDecimal("2500000"), Currency.IDR),
            3,
            TransactionType.PURCHASE,
            "MERCH-9"
        );

        CreateTransactionResult result = workflow.execute(command, now);

        assertThat(result.status()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(result.replayedFromIdempotency()).isFalse();
        assertThat(result.remainingLimit().amount()).isEqualByComparingTo(new BigDecimal("7500000"));
        assertThat(account.availableLimit().amount()).isEqualByComparingTo(new BigDecimal("7500000"));
        assertThat(account.version()).isEqualTo(1L);

        verify(transactionRepository).save(any());
        verify(creditAccountRepository).save(account);
        verify(ledgerRepository).save(any());

        ArgumentCaptor<OutboxAppender.OutboxRecord> outbox = ArgumentCaptor.forClass(OutboxAppender.OutboxRecord.class);
        verify(outboxAppender).appendPending(outbox.capture());
        assertThat(outbox.getValue().eventType()).isEqualTo("transaction.created");

        TransactionCreatedEvent evt = objectMapper.readValue(
            outbox.getValue().payloadJson(),
            TransactionCreatedEvent.class
        );
        assertThat(evt.userId()).isEqualTo(USER_ID);
        assertThat(evt.externalRef()).isEqualTo("ORDER-55");
        assertThat(evt.amount()).isEqualByComparingTo(new BigDecimal("2500000"));
    }

    @Test
    void execute_doesNotPersistTransaction_whenInsufficientLimit() {
        CreditAccount account = new CreditAccount(
            ACCOUNT_ID,
            USER_ID,
            "ACC-001",
            Money.of(new BigDecimal("5000000"), Currency.IDR),
            Money.of(new BigDecimal("400000"), Currency.IDR),
            AccountStatus.ACTIVE,
            0L
        );
        when(creditAccountRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(account));
        when(transactionRepository.existsByExternalRef("ORDER-56")).thenReturn(false);

        CreateTransactionCommand command = new CreateTransactionCommand(
            "idem-2",
            USER_ID,
            "ORDER-56",
            Money.of(new BigDecimal("500000"), Currency.IDR),
            6,
            TransactionType.PURCHASE,
            null
        );

        Instant now = Instant.parse("2026-05-10T10:20:00Z");

        assertThatThrownBy(() -> workflow.execute(command, now))
            .isInstanceOf(InsufficientLimitException.class);

        verify(transactionRepository, never()).save(any());
        verify(ledgerRepository, never()).save(any());
        verify(outboxAppender, never()).appendPending(any());
        verify(creditAccountRepository, never()).save(any());
    }
}
