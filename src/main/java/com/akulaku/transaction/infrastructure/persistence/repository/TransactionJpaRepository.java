package com.akulaku.transaction.infrastructure.persistence.repository;

import com.akulaku.transaction.infrastructure.persistence.jpa.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionJpaRepository extends JpaRepository<TransactionEntity, UUID> {

    boolean existsByExternalRef(String externalRef);
}
