package com.example.bank.controller;

import com.example.bank.model.card.debitCard.*;
import com.example.bank.service.DebitCardService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/debit-cards")
public class DebitCardController {

    private final DebitCardService debitCardService;

    public DebitCardController(DebitCardService debitCardService) {
        this.debitCardService = debitCardService;
    }



    @PostMapping("/{id}")
    @Operation(summary = "Создать дебетовую карту", description = "Создание дебетовой карты для пользователя по ID")
    public ResponseEntity<DebitCardResponse> createCard(@PathVariable Long id) {
        DebitCardResponse created = debitCardService.createCard(id);
        return ResponseEntity.status(201).body(created);
    }



    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить дебетовую карту", description = "Удаление дебетовой карты по ID")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        debitCardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }
}