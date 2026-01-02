package com.example.bank.mapper;

import com.example.bank.model.card.Card;

import com.example.bank.model.card.debitCard.DebitCardResponse;
import com.example.bank.service.CardServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class DebitCardMapper {


    public static DebitCardResponse toDto(Card card) {
        DebitCardResponse dto = new DebitCardResponse();
        dto.setId(card.getId());
        dto.setBalance(card.getBalance());
        dto.setCardNumber(CardServiceImpl.maskCardNumber(card.getCardNumber()));
        dto.setBalance(card.getBalance());
        dto.setCardType(card.getCardType());
        return dto;
    }

}









