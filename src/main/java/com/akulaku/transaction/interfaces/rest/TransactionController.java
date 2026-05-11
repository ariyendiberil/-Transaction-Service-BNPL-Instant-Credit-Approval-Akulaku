package com.akulaku.transaction.interfaces.rest;

import com.akulaku.transaction.domain.model.Currency;
import com.akulaku.transaction.domain.model.Money;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;
import com.akulaku.transaction.interfaces.rest.dto.CreateTransactionRequest;
import com.akulaku.transaction.interfaces.rest.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/transactions")
@Validated
@Tag(name = "Transactions", description = "BNPL transaction operations")
public class TransactionController {

    private final CreateTransactionUseCase createTransactionUseCase;

    public TransactionController(CreateTransactionUseCase createTransactionUseCase) {
        this.createTransactionUseCase = createTransactionUseCase;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a PURCHASE transaction",
        description = "Debits available limit idempotently. Requires Idempotency-Key header."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Created",
        headers = {
            @Header(name = HttpHeaders.LOCATION, description = "URI of the new transaction"),
            @Header(name = "X-Idempotency-Replayed", description = "true if response was served from idempotency cache")
        },
        content = @Content(schema = @Schema(implementation = TransactionResponse.class))
    )
    public ResponseEntity<TransactionResponse> create(
        @RequestHeader("Idempotency-Key")
        @NotBlank(message = "Idempotency-Key header is required")
        @Size(min = 8, max = 128)
        String idempotencyKey,
        @Valid @RequestBody CreateTransactionRequest request
    ) {
        CreateTransactionCommand command = new CreateTransactionCommand(
            idempotencyKey.trim(),
            request.userId(),
            request.externalRef(),
            Money.of(request.amount(), Currency.valueOf(request.currency())),
            request.tenorMonths(),
            request.transactionType(),
            request.merchantId()
        );

        CreateTransactionResult result = createTransactionUseCase.execute(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(result.transactionId())
            .toUri();

        return ResponseEntity.created(location)
            .header("X-Idempotency-Replayed", Boolean.toString(result.replayedFromIdempotency()))
            .body(toResponse(result));
    }

    private static TransactionResponse toResponse(CreateTransactionResult result) {
        return new TransactionResponse(
            result.transactionId(),
            result.status().name(),
            result.remainingLimit().amount(),
            result.remainingLimit().currency().name(),
            result.createdAt(),
            result.replayedFromIdempotency()
        );
    }
}
