package com.akulaku.transaction.domain.port.out;

import com.akulaku.transaction.domain.model.CreditAccount;

import java.util.Optional;
import java.util.UUID;

public interface CreditAccountRepository {

    Optional<CreditAccount> findByUserIdForUpdate(UUID userId);

    void save(CreditAccount account);
}
