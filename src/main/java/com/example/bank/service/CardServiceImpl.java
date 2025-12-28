package com.example.bank.service;

import com.example.bank.model.card.Card;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CardServiceImpl extends AbstractCardService {

    public CardServiceImpl(CardRepository cardRepository,
                              UserRepository userRepository,
                              CardSecurity cardSecurity) {
        super(cardRepository, userRepository, cardSecurity);
    }


    public List<Card> getAllCards() {
        if (cardRepository.findByUser_UserId(getCurrentUser().getUserId()).isEmpty()) {
            return List.of();
        }

        return cardRepository.findByUser_UserId(getCurrentUser().getUserId());

    }
}
