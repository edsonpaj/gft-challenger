package com.dws.challenge.dto;

import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
@Builder
public class AmountTransferDTO {
  @NotNull
  @NotEmpty
  private final String sourceAccountId;
  @NotNull
  @NotEmpty
  private final String destinationAccountId;
  @NotNull
  @NotEmpty
  @Min(0)
  private final BigDecimal transferAmount;

}
