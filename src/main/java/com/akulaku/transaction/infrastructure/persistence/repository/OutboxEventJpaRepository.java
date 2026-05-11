package com.akulaku.transaction.infrastructure.persistence.repository;

import com.akulaku.transaction.infrastructure.persistence.jpa.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {

    @Query(
        value = """
            SELECT * FROM outbox_events
            WHERE status = 'PENDING'
            ORDER BY id ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """,
        nativeQuery = true
    )
    List<OutboxEventEntity> lockPendingBatchForUpdate(@Param("limit") int limit);
}
