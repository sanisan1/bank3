package com.example.bank.model.transaction;

import com.example.bank.Enums.OperationType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;



@Data
@NoArgsConstructor
public class TransactionResponse {
    private Long Id;
    private String fromCard;
    private String toCard;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private OperationType type;
    private String comment;



}