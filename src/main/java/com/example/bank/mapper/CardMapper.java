package com.example.bank.mapper;

import com.example.bank.model.card.Card;
import com.example.bank.model.card.CardDto;

import com.example.bank.service.CardServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class CardMapper {


    public static CardDto toDto(Card card) {
        CardDto dto = new CardDto();
        dto.setId(card.getId());
        dto.setBalance(card.getBalance());
        dto.setCardNumber(CardServiceImpl.maskCardNumber(card.getCardNumber()));
        dto.setBalance(card.getBalance());
        dto.setCardType(card.getCardType());
        return dto;
    }

}









