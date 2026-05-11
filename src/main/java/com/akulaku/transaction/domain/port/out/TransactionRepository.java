package com.akulaku.transaction.domain.port.out;

import com.akulaku.transaction.domain.model.Transaction;

public interface TransactionRepository {

    Transaction save(Transaction transaction);

    boolean existsByExternalRef(String externalRef);
}
