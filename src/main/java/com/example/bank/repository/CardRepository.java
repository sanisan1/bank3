package com.example.bank.repository;

import com.example.bank.Enums.CardStatus;
import com.example.bank.model.card.Card;
import com.example.bank.Enums.CardType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.user.userId = :userId")
    List<Card> findByUser_UserId(@Param("userId") Long userId);

    List<Card> findByCardNumberContaining(String cardNumber);

    List<Card> findByUser_UserIdAndCardNumberContaining(Long userId, String cardNumber);

    Page<Card> findByStatus(CardStatus cardStatus, Pageable pageable);
}
