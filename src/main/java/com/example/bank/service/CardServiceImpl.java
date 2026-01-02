package com.example.bank.service;

import com.example.bank.Enums.CardStatus;
import com.example.bank.Enums.CardType;
import com.example.bank.Enums.Role;
import com.example.bank.mapper.CardMapper;
import com.example.bank.model.card.Card;
import com.example.bank.model.card.CardDto;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CardServiceImpl extends AbstractCardService {

    public CardServiceImpl(CardRepository cardRepository,
                           UserRepository userRepository,
                           CardSecurity cardSecurity) {
        super(cardRepository, userRepository, cardSecurity);
    }

    public static String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "**** **** **** ****";
        }

        String lastFour = cardNumber.substring(cardNumber.length() - 4);
        return "**** **** **** " + lastFour;
    }

    @Transactional
    public List<CardDto> getAllCards() {
        User currentUser = getCurrentUser();
        List<Card> cards = cardRepository.findByUser_UserId(currentUser.getUserId());

        List<CardDto> cardDtos = new ArrayList<>();
        for (Card card : cards) {
            cardDtos.add(CardMapper.toDto(card));
        }

        return cardDtos;
    }

    // Метод для поиска карт с пагинацией (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getAllCardsWithPagination(Pageable pageable) {
        Page<Card> cards = cardRepository.findAll(pageable);
        return cards.map(CardMapper::toDto);
    }

    // Метод для поиска карт по номеру карты (доступен только владельцу или администратору)
    @PreAuthorize("@cardSecurity.isOwner(#cardNumber) or hasRole('ADMIN')")
    public List<CardDto> searchCardsByNumber(String cardNumber) {
        User currentUser = getCurrentUser();

        if (currentUser.getRole() == Role.ADMIN) {
            // Администратор может искать по частичному совпадению номера карты
            List<Card> cards = cardRepository.findByCardNumberContaining(cardNumber);
            return cards.stream()
                    .map(CardMapper::toDto)
                    .collect(Collectors.toList());
        } else {
            // Обычный пользователь может искать только свои карты по номеру
            List<Card> cards = cardRepository.findByUser_UserIdAndCardNumberContaining(
                    currentUser.getUserId(), cardNumber);
            return cards.stream()
                    .map(CardMapper::toDto)
                    .collect(Collectors.toList());
        }
    }
    // Метод для запроса блокировки карты пользователем
    @PreAuthorize("@cardSecurity.isOwner(#id)")
    public Card requestBlockCard(Long id) {
        Card card = getCardById(id);
        User currentUser = getCurrentUser();


        if (!card.getUser().getUserId().equals(currentUser.getUserId())) {
            throw new AccessDeniedException("Пользователь не является владельцем карты");
        }


        card.setStatus(CardStatus.PENDING_BLOCK);
        return cardRepository.save(card);
    }

    // Метод для получения карт со статусом PENDING_BLOCK с пагинацией (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getCardsWithBlockRequestsWithPagination(Pageable pageable) {
        Page<Card> cards = cardRepository.findByStatus(CardStatus.PENDING_BLOCK, pageable);
        return cards.map(CardMapper::toDto);
    }





}
