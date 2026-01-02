package com.example.bank.controller;

import com.example.bank.model.transaction.TransactionOperationRequest;
import com.example.bank.model.transaction.TransactionResponse;

import com.example.bank.model.card.CardDto;
import com.example.bank.model.transaction.TransferRequest;
import com.example.bank.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public CardDto deposit(@RequestBody TransactionOperationRequest request) {
        return transactionService.deposit(
                request.getId(),
                request.getAmount(),
                request.getComment()
        );
    }

    @PostMapping("/withdraw")
    public CardDto withdraw(@RequestBody TransactionOperationRequest request) {
        return transactionService.withdraw(
                request.getId(),
                request.getAmount(),
                request.getComment()
        );
    }

    @PostMapping("/transfer")
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
    public List<TransactionResponse> getTransactionsByCard(@PathVariable String cardNumber) {
        return transactionService.getTransactionsByCard(cardNumber);
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransactionById(@PathVariable Long id) {
        return transactionService.getTransactionById(id);
    }


    @GetMapping("/getAll")
    public List<TransactionResponse> getAllTransactions() {
        return transactionService.getAllTransactions();
    }


    @GetMapping("/by-user/{userId}")
    public List<TransactionResponse> getTransactionsByUser(@PathVariable Long userId) {
        return transactionService.getTransactionsByUser(userId);
    }

    @GetMapping("/getAllForUser")
    public List<TransactionResponse> getTransactionsForUser() {
        return transactionService.getTransactionsForUser();
    }


}