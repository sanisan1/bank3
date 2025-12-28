package com.example.bank.security;

import com.example.bank.Enums.Role;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.model.card.Card;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component("cardSecurity")
public class CardSecurity {
    private static final Logger logger = LoggerFactory.getLogger(CardSecurity.class);
    
    private final CardRepository cardRepository;


    public CardSecurity(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    public boolean isOwner(Long cardId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            logger.warn("No authentication found for card ID: {}", cardId);
            return false;
        }
        
        String username = auth.getName();
        logger.debug("Checking ownership for user: {} and card ID: {}", username, cardId);
        
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "id", cardId));

        boolean isOwner = card.getUser().getUsername().equals(username);
        logger.debug("Ownership check result: {}", isOwner);
        return isOwner;
    }

    public boolean isOwner(String cardNumber) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            logger.warn("No authentication found for card number: {}", cardNumber);
            return false;
        }
        
        String username = auth.getName();
        logger.debug("Checking ownership for user: {} and card number: {}", username, cardNumber);
        
        Card card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "cardNumber", cardNumber));

        boolean isOwner = card.getUser().getUsername().equals(username);
        logger.debug("Ownership check result: {}", isOwner);
        return isOwner;
    }

    public boolean isSelfOrAdmin(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) auth.getPrincipal();
        return currentUser.getRole() == Role.ADMIN || currentUser.getUserId().equals(userId);
    }
}