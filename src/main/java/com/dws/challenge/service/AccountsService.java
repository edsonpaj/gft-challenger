package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.repository.AccountsRepository;
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
    val transferAmount = amountTransferDTO.getTransferAmount();
    if (this.accountsRepository.withdrawMoney(amountTransferDTO.getSourceAccountId(), transferAmount)) {
      this.accountsRepository.depositMoney(amountTransferDTO.getDestinationAccountId(), transferAmount);
      notifySuccessTransfer(amountTransferDTO);
    } else {
      System.out.println("Insufficient funds - transaction not processed - Possible solutions, 1 - Put in a retry strategy. 2 - Send to Dead letter queue.");
    }
  }

  private void notifySuccessTransfer(AmountTransferDTO amountTransferDTO) {
    val sourceAccountUpdated = this.accountsRepository.getAccount(amountTransferDTO.getSourceAccountId());
    final Account accountToNotify = new Account(amountTransferDTO.getSourceAccountId(), sourceAccountUpdated.getBalance().get());
    notificationService.notifyAboutTransfer(accountToNotify,
            "An amount of "
                    + amountTransferDTO.getTransferAmount()
                    + " was transferred from account "
                    + amountTransferDTO.getSourceAccountId()
                    + " to account "
                    + amountTransferDTO.getDestinationAccountId());
  }
}
