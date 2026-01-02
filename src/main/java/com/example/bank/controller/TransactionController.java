package com.example.bank.controller;

import com.example.bank.model.transaction.TransactionOperationRequest;
import com.example.bank.model.transaction.TransactionResponse;

import com.example.bank.model.card.CardDto;
import com.example.bank.model.transaction.TransferRequest;
import com.example.bank.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // Операции
    @PostMapping("/deposit")
    @Operation(summary = "Пополнение карты", description = "Пополнение баланса карты")
    public CardDto deposit(@RequestBody TransactionOperationRequest request) {
        return transactionService.deposit(
                request.getId(),
                request.getAmount(),
                request.getComment()
        );
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Снятие со счёта", description = "Снятие средств с карты")
    public CardDto withdraw(@RequestBody TransactionOperationRequest request) {
        return transactionService.withdraw(
                request.getId(),
                request.getAmount(),
                request.getComment()
        );
    }

    @PostMapping("/transfer")
    @Operation(summary = "Перевод между картами", description = "Перевод средств между картами")
    public CardDto transfer(@RequestBody TransferRequest request) {
        return transactionService.transfer(
                request.getFromId(),
                request.getToId(),
                request.getAmount(),
                request.getComment()
        );
    }

    // Запросы данных
    @GetMapping("/by-card/{cardNumber}")
    @Operation(summary = "Получить транзакции по карте", description = "Получение списка транзакций по номеру карты")
    public List<TransactionResponse> getTransactionsByCard(@PathVariable String cardNumber) {
        return transactionService.getTransactionsByCard(cardNumber);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить транзакцию по ID", description = "Получение транзакции по её ID")
    public TransactionResponse getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }


    @GetMapping("/getAll")
    @Operation(summary = "Получить все транзакции", description = "Получение списка всех транзакций")
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.getAllTransactions();
    }


    @GetMapping("/by-user/{userId}")
    @Operation(summary = "Получить транзакции по пользователю", description = "Получение транзакций по ID пользователя")
    public List<TransactionResponse> getTransactionsByUser(@PathVariable Long userId) {
        return transactionService.getTransactionsByUser(userId);
    }

    @GetMapping("/getAllForUser")
    @Operation(summary = "Получить транзакции для текущего пользователя", description = "Получение транзакций для авторизованного пользователя")
    public List<TransactionResponse> getTransactionsForUser() {
        return transactionService.getTransactionsForUser();
    }


}