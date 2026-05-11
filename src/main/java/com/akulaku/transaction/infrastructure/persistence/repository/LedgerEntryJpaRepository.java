package com.akulaku.transaction.infrastructure.persistence.repository;

import com.akulaku.transaction.infrastructure.persistence.jpa.LedgerEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryEntity, Long> {
}
