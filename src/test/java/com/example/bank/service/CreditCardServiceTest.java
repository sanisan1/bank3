package com.example.bank.service;

import com.example.bank.exception.InvalidOperationException;
import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.repository.*;
import com.example.bank.security.CardSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CreditCardServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CardSecurity cardSecurity;

    @InjectMocks
    private CreditCardService creditCardService;

    private CreditCard card;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        card = new CreditCard();
        card.setCreditLimit(BigDecimal.valueOf(1000));
        card.setBalance(BigDecimal.valueOf(1000));
        card.setDebt(BigDecimal.ZERO);
        card.setAccruedInterest(BigDecimal.ZERO);
        card.setInterestRate(BigDecimal.valueOf(12)); // 12% годовых
        card.setExpiryDate(LocalDate.now().plusYears(1));
        card.setStatus(com.example.bank.Enums.CardStatus.ACTIVE);
    }


    @Test
    void testProcessWithdrawWithinLimit() {
        // Проверка снятия в пределах лимита: баланс уменьшается, долг увеличивается
        creditCardService.processWithdraw(card, BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(800), card.getBalance());
        assertEquals(BigDecimal.valueOf(200), card.getDebt());
    }

    @Test
    void testProcessWithdrawExceedsLimitThrows() {
        // Проверка, что при превышении лимита выбрасывается исключение
        assertThrows(InvalidOperationException.class, () ->
                creditCardService.processWithdraw(card, BigDecimal.valueOf(2000)));
    }

    @Test
    void testProcessDepositReducesDebt() {
        // Проверка, что депозит уменьшает долг и увеличивает баланс
        card.setDebt(BigDecimal.valueOf(300));
        card.setBalance(BigDecimal.valueOf(700));

        creditCardService.processDeposit(card, BigDecimal.valueOf(100));

        assertEquals(BigDecimal.valueOf(800), card.getBalance());
        assertEquals(BigDecimal.valueOf(200), card.getDebt());
    }

    @Test
    void testAccrueMonthlyInterestAddsInterest() {
        // Проверка начисления процентов на тело долга
        card.setDebt(BigDecimal.valueOf(1000));

        Page<CreditCard> page = new PageImpl<>(List.of(card));
        when(creditCardRepository.findAll(any(Pageable.class))).thenReturn(page);

        creditCardService.accrueMonthlyInterest();

        // 12% годовых = 1% в месяц → 10 от 1000
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), card.getAccruedInterest().setScale(2));
        verify(creditCardRepository, atLeastOnce()).save(card);
    }

    @Test
    void testDepositPaysOffInterestAndPartOfDebt() {
        // Проверка погашения процентов и части тела долга при депозите
        card.setTotalDebt(BigDecimal.valueOf(550));   // 500 тело + 50 проценты
        card.setDebt(BigDecimal.valueOf(500));
        card.setAccruedInterest(BigDecimal.valueOf(50));
        card.setBalance(BigDecimal.valueOf(500));

        creditCardService.processDeposit(card, BigDecimal.valueOf(60));

        assertEquals(BigDecimal.ZERO, card.getAccruedInterest());   // проценты погашены
        assertEquals(BigDecimal.valueOf(490), card.getDebt());      // долг уменьшился на 10
        assertEquals(BigDecimal.valueOf(510), card.getBalance());   // баланс вырос на 10
        assertEquals(BigDecimal.valueOf(490), card.getTotalDebt()); // общий долг пересчитан
    }

    @Test
    void testDepositPaysOffPartOfInterestOnly() {
        // Проверка погашения только части процентов при депозите
        card.setTotalDebt(BigDecimal.valueOf(550));   // 500 тело + 50 проценты
        card.setDebt(BigDecimal.valueOf(500));
        card.setAccruedInterest(BigDecimal.valueOf(50));
        card.setBalance(BigDecimal.valueOf(500));

        creditCardService.processDeposit(card, BigDecimal.valueOf(40));

        assertEquals(BigDecimal.valueOf(10), card.getAccruedInterest());
        assertEquals(BigDecimal.valueOf(500), card.getDebt());
        assertEquals(BigDecimal.valueOf(500), card.getBalance());
        assertEquals(BigDecimal.valueOf(510), card.getTotalDebt());
    }

    @Test
    void testWithdrawCreatesDebtCorrectly() {
        // Проверка корректного обновления тела долга при снятии
        card.setBalance(BigDecimal.valueOf(1200));

        creditCardService.processWithdraw(card, BigDecimal.valueOf(300));

        assertEquals(BigDecimal.ZERO, card.getAccruedInterest());
        assertEquals(BigDecimal.valueOf(100), card.getDebt());      // долг после снятия
        assertEquals(BigDecimal.valueOf(900), card.getBalance());   // баланс уменьшился
        assertEquals(BigDecimal.valueOf(100), card.getTotalDebt());
    }

    @Test
    void testAccrueInterestOnEmptyCardDoesNothing() {
        // Проверка, что у нового аккаунта без долга начисление процентов ничего не меняет
        card.setBalance(BigDecimal.valueOf(1200));

        Page<CreditCard> page = new PageImpl<>(List.of(card));
        when(creditCardRepository.findAll(any(Pageable.class))).thenReturn(page);

        creditCardService.accrueMonthlyInterest();

        assertEquals(BigDecimal.ZERO, card.getAccruedInterest());
        assertEquals(BigDecimal.ZERO, card.getDebt());
        assertEquals(BigDecimal.valueOf(1200), card.getBalance());
        assertEquals(BigDecimal.ZERO, card.getTotalDebt());
    }





}
