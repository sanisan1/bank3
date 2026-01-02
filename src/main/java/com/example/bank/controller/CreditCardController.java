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





    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        creditCardService.deleteById(id);
        return ResponseEntity.noContent().build();
    }






    @PutMapping("/{id}/increase-limit")
    public ResponseEntity<CreditCard> increaseCreditLimit(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal newLimit
    ) {
        CreditCard card = creditCardService.increaseCreditLimit(id, newLimit);
        return ResponseEntity.ok(card);
    }


    @PutMapping("/{id}/decrease-limit")
    public ResponseEntity<CreditCard> decreaseCreditLimit(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0.01") BigDecimal newLimit
    ) {
        CreditCard card = creditCardService.decreaseCreditLimit(id, newLimit);
        return ResponseEntity.ok(card);
    }


    @PutMapping("/{id}/set-interest")
    public ResponseEntity<CreditCard> setInterestRate(
            @PathVariable Long id,
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal newRate
    ) {
        CreditCard card = creditCardService.setInterestRate(id, newRate);
        return ResponseEntity.ok(card);
    }


    @PostMapping("/accrue-interest")
    public ResponseEntity<String> runAccrueInterest() {
        creditCardService.accrueMonthlyInterest(); // вызываем метод
        return ResponseEntity.ok("Начисление процентов выполнено");
    }
}
