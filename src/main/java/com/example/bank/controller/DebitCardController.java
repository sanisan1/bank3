package com.example.bank.controller;

import com.example.bank.model.card.debitCard.*;
import com.example.bank.service.DebitCardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debit-cards")
public class DebitCardController {

    private final DebitCardService debitCardService;

    public DebitCardController(DebitCardService debitCardService) {
        this.debitCardService = debitCardService;
    }

    // ✅ Создание аккаунта
    @PostMapping
    public ResponseEntity<DebitCardResponse> createCard() {
        DebitCardResponse created = debitCardService.createCard();
        return ResponseEntity.status(201).body(created);
    }


    // ✅ Удаление аккаунта
    @DeleteMapping("/{cardNumber}")
    public ResponseEntity<Void> deleteCard(@PathVariable String cardNumber) {
        debitCardService.deleteCard(cardNumber);
        return ResponseEntity.noContent().build();
    }
}
