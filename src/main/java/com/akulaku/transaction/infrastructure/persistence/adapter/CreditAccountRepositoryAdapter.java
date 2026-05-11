package com.akulaku.transaction.infrastructure.persistence.adapter;

import com.akulaku.transaction.domain.model.CreditAccount;
import com.akulaku.transaction.domain.model.Currency;
import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.port.out.CreditAccountRepository;
import com.akulaku.transaction.infrastructure.persistence.jpa.CreditAccountEntity;
import com.akulaku.transaction.infrastructure.persistence.repository.CreditAccountJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CreditAccountRepositoryAdapter implements CreditAccountRepository {

    private final CreditAccountJpaRepository jpa;

    public CreditAccountRepositoryAdapter(CreditAccountJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<CreditAccount> findByUserIdForUpdate(UUID userId) {
        return jpa.findByUserIdForUpdate(userId).map(this::toDomain);
    }

    @Override
    public void save(CreditAccount account) {
        CreditAccountEntity entity = jpa.findById(account.id())
            .orElseThrow(() -> new IllegalStateException("Credit account not persisted: " + account.id()));
        apply(account, entity);
        jpa.save(entity);
    }

    private CreditAccount toDomain(CreditAccountEntity entity) {
        Currency currency = Currency.valueOf(entity.getCurrency());
        return new CreditAccount(
            entity.getId(),
            entity.getUserId(),
            entity.getAccountNumber(),
            Money.of(entity.getCreditLimit(), currency),
            Money.of(entity.getAvailableLimit(), currency),
            entity.getStatus(),
            entity.getVersion()
        );
    }

    private void apply(CreditAccount account, CreditAccountEntity entity) {
        entity.setCreditLimit(account.creditLimit().amount());
        entity.setAvailableLimit(account.availableLimit().amount());
        entity.setStatus(account.status());
        entity.setVersion(account.version());
        entity.setUpdatedAt(Instant.now());
    }
}
