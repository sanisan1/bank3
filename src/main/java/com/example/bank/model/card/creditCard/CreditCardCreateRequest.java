package com.example.bank.model.card.creditCard;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardCreateRequest {
    @NotNull
    private Long userId;

    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.01", message = "Credit limit must be greater than zero")
    private BigDecimal creditLimit;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be positive")
    private BigDecimal interestRate;

    @NotNull(message = "Grace period is required")
    private Integer gracePeriod;

}
