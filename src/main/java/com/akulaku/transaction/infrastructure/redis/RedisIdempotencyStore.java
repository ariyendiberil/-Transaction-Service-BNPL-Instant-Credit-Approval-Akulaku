package com.akulaku.transaction.infrastructure.redis;

import com.akulaku.transaction.domain.port.out.IdempotencyStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisIdempotencyStore implements IdempotencyStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration ttl;
    private final String keyPrefix;

    public RedisIdempotencyStore(
        StringRedisTemplate redis,
        ObjectMapper objectMapper,
        @Value("${app.idempotency.ttl-hours:24}") int ttlHours,
        @Value("${app.idempotency.key-prefix:idem:}") String keyPrefix
    ) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.ttl = Duration.ofHours(ttlHours);
        this.keyPrefix = keyPrefix;
    }

    @Override
    public IdempotencyReservation reserve(String key, String requestHash, String endpoint) {
        String redisKey = keyPrefix + key;
        Payload processing = new Payload("PROCESSING", requestHash, endpoint, null, null);
        Boolean reserved = writeIfAbsent(redisKey, processing);
        if (Boolean.TRUE.equals(reserved)) {
            return new IdempotencyReservation(ReservationStatus.NEW, null, null);
        }

        Payload existing = read(redisKey);
        if (!existing.requestHash().equals(requestHash)) {
            return new IdempotencyReservation(ReservationStatus.CONFLICT, null, null);
        }
        if ("COMPLETED".equals(existing.status())) {
            return new IdempotencyReservation(
                ReservationStatus.ALREADY_COMPLETED,
                existing.responseStatus(),
                existing.responseBody()
            );
        }
        return new IdempotencyReservation(ReservationStatus.ALREADY_PROCESSING, null, null);
    }

    @Override
    public void complete(String key, int httpStatus, String responseBody) {
        String redisKey = keyPrefix + key;
        Payload existing = read(redisKey);
        Payload completed = new Payload(
            "COMPLETED",
            existing.requestHash(),
            existing.endpoint(),
            httpStatus,
            responseBody
        );
        write(redisKey, completed);
    }

    @Override
    public void markFailed(String key) {
        redis.delete(keyPrefix + key);
    }

    private Boolean writeIfAbsent(String redisKey, Payload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            return redis.opsForValue().setIfAbsent(redisKey, json, ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Redis idempotency reserve failed", e);
        }
    }

    private void write(String redisKey, Payload payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(redisKey, json, ttl);
        } catch (Exception e) {
            throw new IllegalStateException("Redis idempotency write failed", e);
        }
    }

    private Payload read(String redisKey) {
        try {
            String json = redis.opsForValue().get(redisKey);
            if (json == null) {
                throw new IllegalStateException("Missing idempotency record for key: " + redisKey);
            }
            return objectMapper.readValue(json, Payload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Redis idempotency read failed", e);
        }
    }

    private record Payload(
        String status,
        String requestHash,
        String endpoint,
        Integer responseStatus,
        String responseBody
    ) {
    }
}
