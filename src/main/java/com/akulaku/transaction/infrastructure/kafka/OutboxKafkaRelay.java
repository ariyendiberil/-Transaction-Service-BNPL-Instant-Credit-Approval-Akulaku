package com.akulaku.transaction.infrastructure.kafka;

import com.akulaku.transaction.infrastructure.persistence.jpa.OutboxEventEntity;
import com.akulaku.transaction.infrastructure.persistence.repository.OutboxEventJpaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Relays pending rows from {@code outbox_events} to Kafka (Outbox pattern).
 */
@Component
public class OutboxKafkaRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxKafkaRelay.class);
    private static final long SEND_TIMEOUT_SEC = 5L;

    private final OutboxEventJpaRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;
    private final int maxRetries;
    private final String transactionCreatedTopic;

    public OutboxKafkaRelay(
        OutboxEventJpaRepository outboxRepository,
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${app.outbox.batch-size:100}") int batchSize,
        @Value("${app.outbox.max-retries:5}") int maxRetries,
        @Value("${app.kafka.topics.transaction-created}") String transactionCreatedTopic
    ) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
        this.maxRetries = maxRetries;
        this.transactionCreatedTopic = transactionCreatedTopic;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEventEntity> batch = outboxRepository.lockPendingBatchForUpdate(batchSize);
        for (OutboxEventEntity event : batch) {
            if (!"transaction.created".equals(event.getEventType())) {
                failOrRetry(event, "Unknown event type: " + event.getEventType());
                continue;
            }
            try {
                kafkaTemplate
                    .send(transactionCreatedTopic, event.getAggregateId().toString(), event.getPayload())
                    .get(SEND_TIMEOUT_SEC, TimeUnit.SECONDS);
                event.setStatus("PUBLISHED");
                event.setPublishedAt(Instant.now());
            } catch (ExecutionException | TimeoutException ex) {
                failOrRetry(event, ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                failOrRetry(event, "interrupted");
            }
        }
        outboxRepository.saveAll(batch);
    }

    private void failOrRetry(OutboxEventEntity event, String reason) {
        log.warn("Outbox relay issue for aggregateId={}: {}", event.getAggregateId(), reason);
        short next = (short) (event.getRetryCount() + 1);
        event.setRetryCount(next);
        if (next >= maxRetries) {
            event.setStatus("FAILED");
        }
    }
}
