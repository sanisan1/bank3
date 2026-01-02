package com.example.bank.model.transaction;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class TransferRequest {
    @NotBlank
    Long fromId;
    @NotBlank
    Long toId;
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    BigDecimal amount;
    String comment;

}
