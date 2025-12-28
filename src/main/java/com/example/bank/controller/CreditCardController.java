package com.example.bank.controller;

import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.model.card.creditCard.CreditCardCreateRequest;
import com.example.bank.model.card.creditCard.CreditCardResponseDto;
import com.example.bank.service.CreditCardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/credit-cards")
public class CreditCardController {

    private final CreditCardService creditCardService;

    public CreditCardController(CreditCardService creditCardService) {
        this.creditCardService = creditCardService;
    }

    /* ----------------------- Создание аккаунта (только для админа) ----------------------- */
    @PostMapping("/createforadmin")
    public ResponseEntity<CreditCardResponseDto> createCard(
            @Valid @RequestBody CreditCardCreateRequest request
    ) {
        CreditCardResponseDto dto = creditCardService.createCard(
                request.getUserId(),
                request.getCreditLimit(),
                request.getInterestRate(),
                request.getGracePeriod()

        );
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    // содание для юзера
    @PostMapping("/create")
    public ResponseEntity<CreditCardResponseDto> createCardForSelf() {
        CreditCardResponseDto dto = creditCardService.createCardforUser();
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }


    /* ----------------------- Операции с аккаунтом ----------------------- */



    @DeleteMapping("/{cardNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(@PathVariable String cardNumber) {
        creditCardService.deleteByCardNumber(cardNumber);
        return ResponseEntity.noContent().build();
    }

    /* ----------------------- Админские методы ----------------------- */




    @PutMapping("/{cardNumber}/increase-limit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CreditCard> increaseCreditLimit(
            @PathVariable String cardNumber,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal newLimit
    ) {
        CreditCard card = creditCardService.increaseCreditLimit(cardNumber, newLimit);
        return ResponseEntity.ok(card);
    }


    @PutMapping("/{cardNumber}/decrease-limit")
    public ResponseEntity<CreditCard> decreaseCreditLimit(
            @PathVariable String cardNumber,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal newLimit
    ) {
        CreditCard card = creditCardService.decreaseCreditLimit(cardNumber, newLimit);
        return ResponseEntity.ok(card);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{cardNumber}/set-interest")
    public ResponseEntity<CreditCard> setInterestRate(
            @PathVariable String cardNumber,
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal newRate
    ) {
        CreditCard card = creditCardService.setInterestRate(cardNumber, newRate);
        return ResponseEntity.ok(card);
    }


    @PostMapping("/accrue-interest")
    public ResponseEntity<String> runAccrueInterest() {
        creditCardService.accrueMonthlyInterest(); // вызываем метод
        return ResponseEntity.ok("Начисление процентов выполнено");
    }
}
