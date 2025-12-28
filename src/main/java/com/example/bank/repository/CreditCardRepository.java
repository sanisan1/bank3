package com.example.bank.repository;

import com.example.bank.model.card.creditCard.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreditCardRepository extends JpaRepository<CreditCard, Long> {

    Optional<CreditCard>findByCardNumber(String cardNumber);

    List<CreditCard> findByUserUserId(Long userId);
}
