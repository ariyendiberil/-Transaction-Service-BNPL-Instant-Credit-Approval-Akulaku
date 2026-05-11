package com.akulaku.transaction.infrastructure.persistence.adapter;

import com.akulaku.transaction.domain.port.out.OutboxAppender;
import com.akulaku.transaction.domain.port.out.OutboxAppender.OutboxRecord;
import com.akulaku.transaction.infrastructure.persistence.jpa.OutboxEventEntity;
import com.akulaku.transaction.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public class OutboxRepositoryAdapter implements OutboxAppender {

    private final OutboxEventJpaRepository jpa;

    public OutboxRepositoryAdapter(OutboxEventJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void appendPending(OutboxRecord record) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.setAggregateId(record.aggregateId());
        entity.setAggregateType(record.aggregateType());
        entity.setEventType(record.eventType());
        entity.setPayload(record.payloadJson());
        entity.setStatus("PENDING");
        entity.setRetryCount((short) 0);
        entity.setCreatedAt(Instant.now());
        jpa.save(entity);
    }
}
