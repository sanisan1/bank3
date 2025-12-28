package com.example.bank.mapper;

import com.example.bank.model.card.Card;
import com.example.bank.model.card.CardDto;

import org.springframework.stereotype.Component;

@Component
public class CardMapper {


    public static CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setBalance(card.getBalance());
        dto.setCardNumber(card.getCardNumber());
        dto.setBalance(card.getBalance());
        dto.setCardType(card.getCardType());
        return dto;
    }

}









