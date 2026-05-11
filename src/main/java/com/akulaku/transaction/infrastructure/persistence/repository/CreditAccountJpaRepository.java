package com.akulaku.transaction.infrastructure.persistence.repository;

import com.akulaku.transaction.infrastructure.persistence.jpa.CreditAccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CreditAccountJpaRepository extends JpaRepository<CreditAccountEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditAccountEntity c WHERE c.userId = :userId")
    Optional<CreditAccountEntity> findByUserIdForUpdate(@Param("userId") UUID userId);
}
