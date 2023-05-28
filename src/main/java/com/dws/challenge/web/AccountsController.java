package com.dws.challenge.web;

import com.dws.challenge.domain.Account;
import com.dws.challenge.dto.AmountTransferDTO;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.service.AccountsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/accounts")
@Slf4j
public class AccountsController {

  private final AccountsService accountsService;

  @Autowired
  public AccountsController(AccountsService accountsService) {
    this.accountsService = accountsService;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<Object> createAccount(@RequestBody @Valid Account account) {
    log.info("Creating account {}", account);
    try {
      this.accountsService.createAccount(account);
    } catch (DuplicateAccountIdException daie) {
      return new ResponseEntity<>(daie.getMessage(), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  @GetMapping(path = "/{accountId}")
  public Account getAccount(@PathVariable String accountId) {
    log.info("Retrieving account for id {}", accountId);
    return this.accountsService.getAccount(accountId);
  }

  /**
   * Transfers an amount of money from one account to another.
   *
   * @param amountTransferDTO the transfer details, including source and destination accounts
   *                          Payload sample: <br/>
   * {<br/>
   *   "sourceAccountId": "123456789",<br/>
   *   "destinationAccountId": "987654321",<br/>
   *   "transferAmount": 100.50<br/>
   * }<br/>
   * @return a ResponseEntity with the appropriate HTTP status and response body
   */
  @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/amountTransfer")
  public ResponseEntity<Object> amountTransfer(@RequestBody @Valid AmountTransferDTO amountTransferDTO) {
    log.info("Transferring value {} from account {} to account {}",
            amountTransferDTO.getTransferAmount().toString(),
            amountTransferDTO.getSourceAccountId(),
            amountTransferDTO.getDestinationAccountId());
    try {
      // Attempt to perform the amount transfer using the AccountsService
      this.accountsService.amountTransfer(amountTransferDTO);
    } catch (Exception e) {
      // If an exception occurs during the transfer, return an appropriate error response
      return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
    }
    // Return a successful response indicating the transfer was accepted
    return new ResponseEntity<>(HttpStatus.ACCEPTED);
  }
}
