package com.akulaku.transaction.interfaces.rest.dto;

import com.akulaku.transaction.domain.model.TransactionType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateTransactionRequest(

    @NotNull
    UUID userId,

    @NotBlank
    @Size(min = 3, max = 64)
    @Pattern(regexp = "^[A-Z0-9\\-_]+$", message = "must be alphanumeric with - or _")
    String externalRef,

    @NotNull
    @DecimalMin(value = "1000", message = "must be at least 1000")
    @DecimalMax(value = "50000000", message = "must be at most 50000000")
    BigDecimal amount,

    @NotBlank
    @Pattern(regexp = "IDR|USD|SGD", message = "must be IDR, USD, or SGD")
    String currency,

    @NotNull
    @Min(1)
    @Max(36)
    Integer tenorMonths,

    @NotNull
    TransactionType transactionType,

    @Size(max = 32)
    String merchantId
) {
}
