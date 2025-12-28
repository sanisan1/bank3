package com.example.bank.model.card.debitCard;

import com.example.bank.Enums.CardType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
@Data
@NoArgsConstructor
public class DebitCardResponse {

    private String cardNumber;
    private BigDecimal balance;
    private CardType cardType;

}
