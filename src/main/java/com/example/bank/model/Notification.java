package com.example.bank.model;

import com.example.bank.Enums.NotflicationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // уникальный идентификатор уведомления
    @NotNull
    private Long userId; // кому принадлежит уведомление
    @Enumerated(EnumType.STRING)
    private NotflicationType type; // тип операции: WITHDRAW, DEPOSIT, TRANSFER, FRAUD, INFO
    @NotNull
    private String title; // короткое описание ("Снятие средств", "Зачисление", "Перевод")

    private String cardTransferTo;
    private String cardNumber;

    private String comment;
    @NotNull
    private String message; // более подробное сообщение ("Со cчета ****1234 снято 1000 ₽")

    private Boolean read = false; // отмечено как прочитано

    private LocalDateTime createdAt = LocalDateTime.now();

    private BigDecimal amount;

    private Long referenceId; //id транзакции
}



