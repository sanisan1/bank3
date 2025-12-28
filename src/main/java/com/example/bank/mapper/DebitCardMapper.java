package com.example.bank.mapper;

import com.example.bank.model.card.Card;

import com.example.bank.model.card.debitCard.DebitCardResponse;
import org.springframework.stereotype.Component;

@Component
public class DebitCardMapper {


    public static DebitCardResponse toDto(Card card) {
        DebitCardResponse dto = new DebitCardResponse();
        dto.setBalance(card.getBalance());
        dto.setCardNumber(card.getCardNumber());
        dto.setBalance(card.getBalance());
        dto.setCardType(card.getCardType());
        return dto;
    }

}









