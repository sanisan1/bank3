package com.example.bank.model;

import com.example.bank.Enums.NotflicationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NotificationResponse {

    private NotflicationType type;
    private String title;
    private String cardTransferTo;
    private String cardNumber;
    private String comment;
    private String message;
    private Boolean read;
    private LocalDateTime createdAt;
    private BigDecimal amount;
    private Long referenceId;
}
