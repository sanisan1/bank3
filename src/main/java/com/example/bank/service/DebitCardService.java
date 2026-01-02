package com.example.bank.service;

import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.mapper.DebitCardMapper;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.model.card.debitCard.DebitCardResponse;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DebitCardService extends AbstractCardService {

    private static final Logger log = LoggerFactory.getLogger(DebitCardService.class);

    public DebitCardService(CardRepository cardRepository,
                               UserRepository userRepository,
                               CardSecurity cardSecurity) {
        super(cardRepository, userRepository, cardSecurity);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public DebitCardResponse createCard(Long userId) {
        log.info("Creating debit card");
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        log.error("User not found with id={}", userId);
                        return new ResourceNotFoundException("User", "id", userId);
                    });

            DebitCard card = new DebitCard();
            card.setUser(user);
            card.setCardNumber(generateUniqueCardNumber());
            card.setExpiryDate(LocalDate.now().plusYears(5));

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
