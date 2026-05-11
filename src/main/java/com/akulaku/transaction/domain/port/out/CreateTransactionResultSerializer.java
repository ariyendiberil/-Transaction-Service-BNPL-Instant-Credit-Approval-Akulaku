package com.akulaku.transaction.domain.port.out;

import com.akulaku.transaction.domain.port.in.CreateTransactionUseCase;

public interface CreateTransactionResultSerializer {

    String toJson(CreateTransactionUseCase.CreateTransactionResult result);

    CreateTransactionUseCase.CreateTransactionResult fromJson(String json);
}
