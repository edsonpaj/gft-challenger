package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
public class AccountsRepositoryInMemory implements AccountsRepository {

    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public void createAccount(Account account) throws DuplicateAccountIdException {
        Account previousAccount = accounts.putIfAbsent(account.getAccountId(), account);
        if (previousAccount != null) {
            throw new DuplicateAccountIdException(
                    "Account id " + account.getAccountId() + " already exists!");
        }
    }

    @Override
    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    @Override
    public void clearAccounts() {
        accounts.clear();
    }

    @Override
    public boolean withdrawMoney(String accountId, BigDecimal amount) {
        AtomicBoolean transactionDone = new AtomicBoolean(false);
        accounts.computeIfPresent(accountId, (key, accountTarget) -> {
            if (isPositive(amount) && haveSufficientFunds(amount, accountTarget)) {
                transactionDone.set(true);
                return new Account(accountTarget.getAccountId(), accountTarget.getBalance().get().subtract(amount));
            } else {
                return accountTarget;
            }
        });
        return transactionDone.get();
    }

    @Override
    public boolean depositMoney(String accountId, BigDecimal amount) {
        AtomicBoolean transactionDone = new AtomicBoolean(false);
        accounts.computeIfPresent(accountId, (key, accountTarget) -> {
            if (isPositive(amount)) {
                transactionDone.set(true);
                return new Account(accountTarget.getAccountId(), accountTarget.getBalance().get().add(amount));
            } else {
                return accountTarget;
            }
        });
        return transactionDone.get();
    }

    private static boolean haveSufficientFunds(BigDecimal amount, Account accountTarget) {
        return accountTarget.getBalance().get().compareTo(amount) > 0;
    }

    private static boolean isPositive(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }

}
