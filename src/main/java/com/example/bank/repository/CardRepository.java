package com.example.bank.repository;

import com.example.bank.model.card.Card;
import com.example.bank.Enums.CardType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    boolean existsById(Long cardNumber);
    List<Card> findByUserUserId(Long userId);

    boolean existsByCardNumber(String number);

    Optional<Card> findByCardNumber(String cardNumber);

    Optional<Card> findByCardNumberAndCardType(String cardNumber, CardType type);

    void deleteByCardNumber(String cardNumber);

    List<Card> findByUserUserIdAndCardType(Long userId, String cardType);

    List<Card> findByUser_UserId(Long userId);
}
