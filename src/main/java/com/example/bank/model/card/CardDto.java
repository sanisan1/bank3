package com.example.bank.model.card;

import com.example.bank.Enums.CardType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class CardDto {

    private String cardNumber;
    private BigDecimal balance;
    private CardType cardType;

}