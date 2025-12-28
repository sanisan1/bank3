package com.example.bank.kafka;

import com.example.bank.Enums.OperationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EventDTO {
    @NotNull
    private Long transactionId;

    @NotNull
    private OperationType type;          // WITHDRAW / DEPOSIT / TRANSFER

    @NotNull
    private String cardNumber; // "4724118063"

    private String cardTransferTo;

    @NotNull
    private BigDecimal amount;    // 100

    @NotNull
    private Long userId;

    String comment;

    private LocalDateTime timestamp = LocalDateTime.now();


}
