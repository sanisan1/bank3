package com.example.bank.exception;

import com.example.bank.model.card.Card;


public class CardBlockedException extends RuntimeException {
    public CardBlockedException(Card card) {
        super("Operation failed user: " + card.getCardNumber() + " is blocked");
    }
}
