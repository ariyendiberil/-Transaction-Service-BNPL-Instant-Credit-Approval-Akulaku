package com.akulaku.transaction.infrastructure.persistence.adapter;

import com.akulaku.transaction.domain.model.LedgerEntry;
import com.akulaku.transaction.domain.port.out.LedgerRepository;
import com.akulaku.transaction.infrastructure.persistence.jpa.LedgerEntryEntity;
import com.akulaku.transaction.infrastructure.persistence.repository.LedgerEntryJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final LedgerEntryJpaRepository jpa;

    public LedgerRepositoryAdapter(LedgerEntryJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public LedgerEntry save(LedgerEntry entry) {
        LedgerEntryEntity entity = toEntity(entry);
        LedgerEntryEntity saved = jpa.save(entity);
        return LedgerEntry.debit(
            saved.getId(),
            saved.getTransactionId(),
            saved.getAccountId(),
            entry.amount(),
            entry.balanceAfter(),
            saved.getCreatedAt()
        );
    }

    private LedgerEntryEntity toEntity(LedgerEntry entry) {
        LedgerEntryEntity e = new LedgerEntryEntity();
        e.setTransactionId(entry.transactionId());
        e.setAccountId(entry.accountId());
        e.setEntryType(entry.entryType());
        e.setAmount(entry.amount().amount());
        e.setBalanceAfter(entry.balanceAfter().amount());
        e.setCurrency(entry.amount().currency().name());
        e.setCreatedAt(entry.createdAt());
        return e;
    }
}
