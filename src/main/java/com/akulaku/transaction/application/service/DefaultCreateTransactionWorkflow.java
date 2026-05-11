package com.akulaku.transaction.application.service;

import com.akulaku.transaction.domain.event.TransactionCreatedEvent;
import com.akulaku.transaction.domain.exception.AccountNotFoundException;
import com.akulaku.transaction.domain.exception.DuplicateExternalReferenceException;
import com.akulaku.transaction.domain.model.CreditAccount;
import com.akulaku.transaction.domain.model.LedgerEntry;
import com.akulaku.transaction.domain.model.Transaction;
import com.akulaku.transaction.domain.model.TransactionType;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;
import com.akulaku.transaction.domain.port.out.CreditAccountRepository;
import com.akulaku.transaction.domain.port.out.LedgerRepository;
import com.akulaku.transaction.domain.port.out.OutboxAppender;
import com.akulaku.transaction.domain.port.out.OutboxAppender.OutboxRecord;
import com.akulaku.transaction.domain.port.out.TransactionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Orchestrates persistence within one transaction ({@code @Transactional}) for ACID guarantees.
 */
@Service
public class DefaultCreateTransactionWorkflow implements CreateTransactionWorkflow {

    private final CreditAccountRepository creditAccountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerRepository ledgerRepository;
    private final OutboxAppender outboxAppender;
    private final ObjectMapper objectMapper;

    public DefaultCreateTransactionWorkflow(
        CreditAccountRepository creditAccountRepository,
        TransactionRepository transactionRepository,
        LedgerRepository ledgerRepository,
        OutboxAppender outboxAppender,
        ObjectMapper objectMapper
    ) {
        this.creditAccountRepository = creditAccountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerRepository = ledgerRepository;
        this.outboxAppender = outboxAppender;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CreateTransactionResult execute(CreateTransactionCommand command, Instant now) {
        if (command.transactionType() != TransactionType.PURCHASE) {
            throw new IllegalArgumentException("Only PURCHASE transactions are supported by this workflow");
        }
        if (transactionRepository.existsByExternalRef(command.externalRef())) {
            throw new DuplicateExternalReferenceException(command.externalRef());
        }

        CreditAccount account = creditAccountRepository
            .findByUserIdForUpdate(command.userId())
            .orElseThrow(() -> new AccountNotFoundException(command.userId()));

        account.debit(command.amount());
        account.bumpVersion();

        Transaction tx = Transaction.newPurchase(
            account.id(),
            command.externalRef(),
            command.idempotencyKey(),
            command.amount(),
            command.tenorMonths(),
            command.merchantId(),
            now
        );
        tx.markApproved(now);

        transactionRepository.save(tx);
        creditAccountRepository.save(account);

        LedgerEntry ledgerEntry = LedgerEntry.newDebit(
            tx.id(),
            account.id(),
            command.amount(),
            account.availableLimit(),
            now
        );
        ledgerRepository.save(ledgerEntry);

        appendOutbox(tx, account, command);

        return new CreateTransactionResult(
            tx.id(),
            tx.status(),
            account.availableLimit(),
            tx.createdAt(),
            false
        );
    }

    private void appendOutbox(Transaction tx, CreditAccount account, CreateTransactionCommand command) {
        TransactionCreatedEvent evt = new TransactionCreatedEvent(
            tx.id(),
            account.id(),
            account.userId(),
            command.externalRef(),
            command.amount().amount(),
            command.amount().currency().name()
        );
        try {
            String payload = objectMapper.writeValueAsString(evt);
            outboxAppender.appendPending(
                new OutboxRecord(tx.id(), "Transaction", "transaction.created", payload)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox event", e);
        }
    }
}
