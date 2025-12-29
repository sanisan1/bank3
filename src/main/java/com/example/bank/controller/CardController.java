package com.example.bank.controller;


import com.example.bank.model.card.Card;

import com.example.bank.service.CardServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardServiceImpl cardService;

    public CardController(CardServiceImpl cardService) {
        this.cardService = cardService;
    }
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{cardNumber}/block")
    public ResponseEntity blockCard(@PathVariable Long id) {
        Card card = cardService.blockCard(cardNumber);
        return ResponseEntity.ok(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{cardNumber}/unblock")
    public ResponseEntity unblockCard(@PathVariable String cardNumber) {
        Card card = cardService.unblockCard(cardNumber);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/getCards")
    public ResponseEntity<List<Card>> getAllCards() {
        List<Card> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/getByNumber/{cardNumber}")
    public ResponseEntity<Card> getCardByNumber(@PathVariable String cardNumber) {
        Card card = cardService.getCardByNumber(cardNumber);
        return ResponseEntity.ok(card);
    }





}
