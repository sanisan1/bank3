package com.example.bank.controller;


import com.example.bank.model.card.Card;

import com.example.bank.model.card.CardDto;
import com.example.bank.service.CardServiceImpl;
import com.example.bank.service.DebitCardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardServiceImpl cardService;
    private final DebitCardService debitCardService;

    public CardController(CardServiceImpl cardService, DebitCardService debitCardService) {
        this.cardService = cardService;
        this.debitCardService = debitCardService;
    }

    @PostMapping("/{id}/block")
    @Operation(summary = "Блокировка карты", description = "Блокировка карты по ID")
    public ResponseEntity blockCard(@PathVariable Long id) {
        Card card = cardService.blockCard(id);
        return ResponseEntity.ok(card);
    }

    @PostMapping("/{id}/unblock")
    public ResponseEntity unblockCard(@PathVariable Long id) {
        Card card = cardService.unblockCard(id);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/getCards")
    @Operation(summary = "Получить все карты", description = "Получение списка всех карт пользователя")
    public ResponseEntity<List<CardDto>> getCards() {
        List<CardDto> cards = cardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/getByNumber/{cardNumber}")
    @Operation(summary = "Получить карту по номеру", description = "Получение карты по её номеру")
    public ResponseEntity<Card> getCardByNumber(@PathVariable String cardNumber) {
        Card card = cardService.getCardByNumber(cardNumber);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/getById/{id}")
    @Operation(summary = "Получить карту по ID", description = "Получение карты по её ID")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        Card card = cardService.getCardById(id);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/all")
    @Operation(summary = "Получить все карты с пагинацией", description = "Получение всех карт с пагинацией и сортировкой")
    public ResponseEntity<Page<CardDto>> getAllCardsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<CardDto> cards = cardService.getAllCardsWithPagination(pageable);
        return ResponseEntity.ok(cards);
    }


    // Поиск карт по номеру (владелец или админ)
    @GetMapping("/search/number")
    @Operation(summary = "Поиск карт по номеру", description = "Поиск карт по номеру карты")
    public ResponseEntity<List<CardDto>> searchCardsByNumber(@RequestParam String cardNumber) {
        List<CardDto> cards = cardService.searchCardsByNumber(cardNumber);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/{id}/request-block")
    @Operation(summary = "Запрос на блокировку карты", description = "Отправка запроса на блокировку карты")
    public ResponseEntity<Card> requestBlockCard(@PathVariable Long id) {
        Card card = cardService.requestBlockCard(id);
        return ResponseEntity.ok(card);
    }
    @GetMapping("/block-requests")
    @Operation(summary = "Запросы на блокировку с пагинацией", description = "Получение запросов на блокировку карт с пагинацией")
    public ResponseEntity<Page<CardDto>> getCardsWithBlockRequestsWithPagination(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        Page<CardDto> cards = cardService.getCardsWithBlockRequestsWithPagination(pageable);
        return ResponseEntity.ok(cards);
    }




}