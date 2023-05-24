package com.dws.challenge.service.thread;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.NotFoundFundsException;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
public class TransferThread implements Runnable {
    private Account sourceAccount;
    private Account destinationAccount;
    private final BigDecimal transferAmount;

    @Override
    public void run() {
        if (sourceAccount.getBalance().get().compareTo(transferAmount) > 0) {
            sourceAccount.getBalance().getAndUpdate(sourceValue -> sourceValue.subtract(transferAmount));
            destinationAccount.getBalance().getAndUpdate(destinationValue -> destinationValue.add(transferAmount));
        } else {
            System.out.println("Insufficient funds - transaction not processed - Possible solutions, 1 - Put in a retry strategy. 2 - Send to Dead letter queue.");
        }
    }
}
