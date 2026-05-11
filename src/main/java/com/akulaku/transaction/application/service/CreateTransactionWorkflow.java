package com.akulaku.transaction.application.service;

import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionCommand;
import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase.CreateTransactionResult;

import java.time.Instant;

@FunctionalInterface
public interface CreateTransactionWorkflow {

    CreateTransactionResult execute(CreateTransactionCommand command, Instant now);
}
