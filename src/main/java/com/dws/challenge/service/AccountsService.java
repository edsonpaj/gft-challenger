package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.exception.NotFoundFundsException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.thread.TransferThread;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;
  @Getter
  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  public void amountTransfer(AmountTransferDTO amountTransferDTO) {
    var sourceAccount = accountsRepository.getAccount(amountTransferDTO.getSourceAccountId());
    var destinationAccount = accountsRepository.getAccount(amountTransferDTO.getDestinationAccountId());
    val transferAmount = amountTransferDTO.getTransferAmount();

    TransferThread transferTransaction = new TransferThread(sourceAccount, destinationAccount, transferAmount);
    val t = new Thread(transferTransaction);
    t.start();

    try {
      t.join();
      final Account accountToNotify = new Account(sourceAccount.getAccountId(), sourceAccount.getBalance().get());
      notificationService.notifyAboutTransfer(accountToNotify,
              "An amount of "
                      + transferAmount.toString()
                      + " was transferred from account "
                      + amountTransferDTO.getSourceAccountId()
                      + " to account "
                      + amountTransferDTO.getDestinationAccountId());
    } catch (NotFoundFundsException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
