package com.akulaku.transaction.application.support;

import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public final class IdempotencyHasher {

    public String hash(CreateTransactionCommand command) {
        String tenor = command.tenorMonths() == null ? "" : String.valueOf(command.tenorMonths());
        String merchant = command.merchantId() == null ? "" : command.merchantId();
        String normalized = String.join(
            "|",
            command.userId().toString(),
            command.externalRef(),
            command.amount().amount().toPlainString(),
            command.amount().currency().name(),
            tenor,
            command.transactionType().name(),
            merchant
        );
        return sha256Hex(normalized);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
