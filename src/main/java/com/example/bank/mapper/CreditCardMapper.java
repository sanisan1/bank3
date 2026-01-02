package com.example.bank.mapper;

import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.model.card.creditCard.CreditCardResponseDto;
import com.example.bank.service.CardServiceImpl;

public class CreditCardMapper {

    // Преобразование из CreditCard в DTO
    public static CreditCardResponseDto toDto(CreditCard creditCard) {
        if (creditCard == null) {
            return null;
        }

        return new CreditCardResponseDto(
                creditCard.getId(),
                CardServiceImpl.maskCardNumber(creditCard.getCardNumber()),
                        creditCard.getBalance(),
                        creditCard.getCreditLimit(),
                        creditCard.getInterestRate(),
                        creditCard.getMinimumPaymentRate(),
                        creditCard.getGracePeriod(),
                        creditCard.getDebt(), // долг
                        creditCard.getPaymentDueDate()
                );
    }
}
