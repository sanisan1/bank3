package com.example.bank.model.transaction;

import com.example.bank.Enums.OperationType;
import com.example.bank.model.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fromCard; // отправитель
    private String toCard;   //получатель
    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;
    private LocalDateTime timestamp = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    @NotNull
    private OperationType type;
    private String comment;
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;


}
