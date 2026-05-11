package com.akulaku.transaction.infrastructure.persistence.adapter;

import com.akulaku.transaction.domain.model.Transaction;
import com.akulaku.transaction.domain.port.out.TransactionRepository;
import com.akulaku.transaction.infrastructure.persistence.jpa.TransactionEntity;
import com.akulaku.transaction.infrastructure.persistence.repository.TransactionJpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public class TransactionRepositoryAdapter implements TransactionRepository {

    private final TransactionJpaRepository jpa;

    public TransactionRepositoryAdapter(TransactionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Transaction save(Transaction transaction) {
        TransactionEntity entity = toEntity(transaction);
        jpa.save(entity);
        return transaction;
    }

    @Override
    public boolean existsByExternalRef(String externalRef) {
        return jpa.existsByExternalRef(externalRef);
    }

    private TransactionEntity toEntity(Transaction tx) {
        TransactionEntity e = new TransactionEntity();
        e.setId(tx.id());
        e.setAccountId(tx.accountId());
        e.setExternalRef(tx.externalRef());
        e.setIdempotencyKey(tx.idempotencyKey());
        e.setTransactionType(tx.type());
        e.setAmount(tx.amount().amount());
        e.setCurrency(tx.amount().currency().name());
        e.setTenorMonths(tx.tenorMonths());
        e.setMerchantId(tx.merchantId());
        e.setStatus(tx.status());
        e.setFailureReason(tx.failureReason());
        e.setCreatedAt(tx.createdAt());
        e.setUpdatedAt(tx.updatedAt());
        return e;
    }
}
