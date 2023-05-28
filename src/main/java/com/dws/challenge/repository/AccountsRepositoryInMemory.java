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

    /**
     * Withdraws a specified amount of money from the account with the given account ID.
     *
     * @param accountId the ID of the account from which the money should be withdrawn
     * @param amount    the amount of money to be withdrawn
     * @return true if the withdrawal was successful, false otherwise
     */
    @Override
    public boolean withdrawMoney(String accountId, BigDecimal amount) {
        AtomicBoolean transactionDone = new AtomicBoolean(false);
        // Use computeIfPresent to update the account balance if the account exists
        accounts.computeIfPresent(accountId, (key, accountTarget) -> {
            if (isPositive(amount) && haveSufficientFunds(amount, accountTarget)) {
                // If the amount is positive and there are sufficient funds, perform the withdrawal
                transactionDone.set(true);
                return new Account(accountTarget.getAccountId(), accountTarget.getBalance().get().subtract(amount));
            } else {
                // If the amount is negative or there are insufficient funds, do not perform the withdrawal
                return accountTarget;
            }
        });

        return transactionDone.get();
    }

    /**
     * Deposits a specified amount of money into the account with the given account ID.
     *
     * @param accountId the ID of the account where the money should be deposited
     * @param amount    the amount of money to be deposited
     * @return true if the deposit was successful, false otherwise
     */
    @Override
    public boolean depositMoney(String accountId, BigDecimal amount) {
        AtomicBoolean transactionDone = new AtomicBoolean(false);
        // Use computeIfPresent to update the account balance if the account exists
        accounts.computeIfPresent(accountId, (key, accountTarget) -> {
            if (isPositive(amount)) {
                // If the amount is positive, perform the deposit
                transactionDone.set(true);
                return new Account(accountTarget.getAccountId(), accountTarget.getBalance().get().add(amount));
            } else {
                // If the amount is negative, do not perform the deposit
                return accountTarget;
            }
        });
        return transactionDone.get();
    }

    /**
     * Checks if the account has sufficient funds to perform a transaction with the specified amount.
     *
     * @param amount        the amount of money involved in the transaction
     * @param accountTarget the account on which the transaction is performed
     * @return true if the account has sufficient funds, false otherwise
     */
    private static boolean haveSufficientFunds(BigDecimal amount, Account accountTarget) {
        return accountTarget.getBalance().get().compareTo(amount) > 0;
    }

    /**
     * Checks if the specified amount is positive.
     *
     * @param amount the amount of money to check
     * @return true if the amount is positive, false otherwise
     */
    private static boolean isPositive(BigDecimal amount) {
        return amount.compareTo(BigDecimal.ZERO) > 0;
    }
}
