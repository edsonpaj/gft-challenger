/**
 * A service class that handles account-related operations, such as creating accounts,
 * retrieving account information, and transferring amounts between accounts.
 */
package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountsService {

  /**
   * The repository used for accessing and persisting account data.
   */
  @Getter
  private final AccountsRepository accountsRepository;

  /**
   * The service used for sending notifications.
   */
  @Getter
  private final NotificationService notificationService;

  /**
   * Constructs an instance of the AccountsService class.
   *
   * @param accountsRepository  the repository for account operations
   * @param notificationService the service for sending notifications
   */
  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  /**
   * Creates a new account.
   *
   * @param account the account to be created
   */
  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  /**
   * Retrieves an account by its ID.
   *
   * @param accountId the ID of the account to retrieve
   * @return the account with the specified ID, or null if not found
   */
  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Transfers an amount of money from one account to another.
   *
   * @param amountTransferDTO the transfer details, including source and destination accounts
   */
  public void amountTransfer(AmountTransferDTO amountTransferDTO) {
    // Retrieve the transfer amount from the transfer details
    val transferAmount = amountTransferDTO.getTransferAmount();

    // Check if the source account has sufficient funds to withdraw the transfer amount
    if (this.accountsRepository.withdrawMoney(amountTransferDTO.getSourceAccountId(), transferAmount)) {
      // Withdraw the transfer amount from the source account
      this.accountsRepository.depositMoney(amountTransferDTO.getDestinationAccountId(), transferAmount);

      // Notify about the successful transfer
      notifySuccessTransfer(amountTransferDTO);
    } else {
      // If the source account does not have sufficient funds, display an error message
      log.info("Insufficient funds - transaction not processed - Possible solutions, 1 - Put in a retry strategy. 2 - Send to Dead letter queue.");
    }
  }

  /**
   * Notifies about a successful amount transfer.
   *
   * @param amountTransferDTO the transfer details, including source and destination accounts
   */
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
