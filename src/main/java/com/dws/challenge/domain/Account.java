package com.dws.challenge.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
public class Account {

  @NotNull
  @NotEmpty
  private final String accountId;

  @NotNull
  private AtomicReference<BigDecimal> balance = new AtomicReference<BigDecimal>(BigDecimal.ZERO);

  public void setBalance(BigDecimal newBalance) {
    if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Initial balance must be positive.");
    }
    balance.set(newBalance);
  }

  public Account(String accountId) {
    this.accountId = accountId;
    this.balance.set(BigDecimal.ZERO);
  }

  @JsonCreator
  public Account(@JsonProperty("accountId") String accountId,
    @JsonProperty("balance") BigDecimal balance) {
    if (balance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("Initial balance must be positive.");
    }
    this.accountId = accountId;
    this.balance.set(balance);
  }
}
