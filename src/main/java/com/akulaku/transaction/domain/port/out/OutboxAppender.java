package com.akulaku.transaction.domain.port.out;

import java.util.UUID;

public interface OutboxAppender {

    void appendPending(OutboxRecord record);

    record OutboxRecord(UUID aggregateId, String aggregateType, String eventType, String payloadJson) {
    }
}
