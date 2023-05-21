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

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE, path = "/amountTransfer")
    public ResponseEntity<Object> amountTransfer(@RequestBody @Valid AmountTransferDTO amountTransferDTO) {
      log.info("Transferring value {} from account {} to account {}",
              amountTransferDTO.getTransferAmount().toString(),
              amountTransferDTO.getSourceAccountId(),
              amountTransferDTO.getDestinationAccountId());
      try {
        this.accountsService.amountTransfer(amountTransferDTO);
      } catch (Exception e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.FORBIDDEN);
      }
      return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }
  }
