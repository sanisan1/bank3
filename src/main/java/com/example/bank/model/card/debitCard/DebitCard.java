package com.example.bank.model.card.debitCard;

import com.example.bank.model.card.Card;
import com.example.bank.Enums.CardType;
import jakarta.persistence.*;
@Entity
public class DebitCard extends Card {
    public DebitCard() { super();
        setCardType(CardType.DEBIT);
    }



}
