package com.example.bank.model.card;


import com.example.bank.Enums.CardStatus;
import com.example.bank.Enums.CardType;
import com.example.bank.model.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "cards")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private long id;

    @Column(length = 10, unique = true, nullable = false)
    private String cardNumber;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    @NotNull
    private User user;

    @Column(nullable = false)
    private LocalDate expiryDate;

    private BigDecimal balance = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    private CardStatus status = CardStatus.ACTIVE;


    @Enumerated(EnumType.STRING)
    private CardType cardType;


}
