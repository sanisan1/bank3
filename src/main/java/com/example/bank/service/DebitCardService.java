package com.example.bank.service;

import com.example.bank.mapper.DebitCardMapper;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.model.card.debitCard.DebitCardResponse;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DebitCardService extends AbstractCardService {

    private static final Logger log = LoggerFactory.getLogger(DebitCardService.class);

    public DebitCardService(CardRepository cardRepository,
                               UserRepository userRepository,
                               CardSecurity cardSecurity) {
        super(cardRepository, userRepository, cardSecurity);
    }

    // Создание дебетвого аккаунта
    public DebitCardResponse createCard() {
        log.info("Creating debit card");
        try {
            User user = getCurrentUser();

            DebitCard card = new DebitCard();
            card.setUser(user);
            card.setCardNumber(generateUniqueCardNumber());
            DebitCard saved = cardRepository.save(card);

            if (user.getMainCard() == null) {
                user.setMainCard(saved);
                log.info("Assigned as main card: {}", saved.getCardNumber());
            }

            return DebitCardMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating debit card: {}", e.getMessage(), e);
            throw e;
        }
    }
}
