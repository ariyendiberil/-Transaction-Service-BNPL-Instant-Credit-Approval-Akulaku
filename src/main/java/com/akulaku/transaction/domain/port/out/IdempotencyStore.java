package com.akulaku.transaction.domain.port.out;

public interface IdempotencyStore {

    IdempotencyReservation reserve(String key, String requestHash, String endpoint);

    void complete(String key, int httpStatus, String responseBody);

    void markFailed(String key);

    enum ReservationStatus {
        NEW,
        ALREADY_COMPLETED,
        ALREADY_PROCESSING,
        CONFLICT
    }

    record IdempotencyReservation(
        ReservationStatus status,
        Integer cachedHttpStatus,
        String cachedResponseBody
    ) {
    }
}
