package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.exception.NotFoundFundsException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

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

  public synchronized void amountTransfer(AmountTransferDTO amountTransferDTO) {
    var sourceAccount = accountsRepository.getAccount(amountTransferDTO.getSourceAccountId());
    var destinationAccount = accountsRepository.getAccount(amountTransferDTO.getDestinationAccountId());
    if(sourceAccount.getBalance().compareTo(amountTransferDTO.getTransferAmount()) > 0){
      sourceAccount.setBalance(sourceAccount.getBalance().subtract(amountTransferDTO.getTransferAmount()));
      destinationAccount.setBalance(destinationAccount.getBalance().add(amountTransferDTO.getTransferAmount()));

      final Account accountToNotify = new Account(sourceAccount.getAccountId(), sourceAccount.getBalance());

      notificationService.notifyAboutTransfer(accountToNotify,
              "An amount of "
                      +amountTransferDTO.getTransferAmount().toString()
                      +" was transferred from account "
                      +amountTransferDTO.getSourceAccountId()
                      +" to account "
                      +amountTransferDTO.getDestinationAccountId());
    } else {
      throw new NotFoundFundsException("Insufficient funds to transfer.");
    }
  }
}
