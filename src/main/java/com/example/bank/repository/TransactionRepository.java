package com.example.bank.repository;


import com.example.bank.model.transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromCardOrToCard(String fromCard, String toCard);
    List<Transaction> findByFromCardInOrToCardIn(List<String> fromCards, List<String> toCards);
}
