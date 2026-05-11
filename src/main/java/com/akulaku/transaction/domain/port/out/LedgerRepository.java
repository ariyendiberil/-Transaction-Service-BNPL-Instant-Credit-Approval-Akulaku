package com.akulaku.transaction.domain.port.out;

import com.akulaku.transaction.domain.model.LedgerEntry;

public interface LedgerRepository {

    LedgerEntry save(LedgerEntry entry);
}
