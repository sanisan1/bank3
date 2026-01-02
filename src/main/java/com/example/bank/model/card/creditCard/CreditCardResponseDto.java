package com.example.bank.model.card.creditCard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardResponseDto {
    private Long id;
    private String cardNumber;
    private BigDecimal balance;
    private BigDecimal creditLimit;
    private BigDecimal interestRate;
    private BigDecimal minimumPaymentRate;
    private Integer gracePeriod;
    private BigDecimal totalDebt; // показываем только totalDebt
    private LocalDate paymentDueDate;


}
