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


    /**
     * Проверяет, что при снятии суммы в пределах кредитного лимита:
     * - баланс уменьшается на сумму;
     * - тело долга увеличивается на ту же сумму.
     */
    @Test
    void testProcessWithdrawWithinLimit() {
        creditCardService.processWithdraw(card, BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(800), card.getBalance());
        assertEquals(BigDecimal.valueOf(200), card.getDebt());
    }

    /**
     * Проверяет, что при попытке снять больше, чем доступно по лимиту,
     * выбрасывается исключение InvalidOperationException.
     */
    @Test
    void testProcessWithdrawExceedsLimitThrows() {
        assertThrows(InvalidOperationException.class, () ->
                creditCardService.processWithdraw(card, BigDecimal.valueOf(2000)));
    }

    /**
     * Проверяет, что при внесении денег на счёт без процентов:
     * - долг уменьшается на сумму пополнения;
     * - баланс увеличивается.
     */
    @Test
    void testProcessDepositReducesDebt() {
        card.setDebt(BigDecimal.valueOf(300));
        card.setBalance(BigDecimal.valueOf(700));

        creditCardService.processDeposit(card, BigDecimal.valueOf(100));

        assertEquals(BigDecimal.valueOf(800), card.getBalance());
        assertEquals(BigDecimal.valueOf(200), card.getDebt());
    }

    /**
     * Проверяет начисление процентов на тело долга.
     * Процент начисляется только на Debt, не на проценты.
     */
    @Test
    void testAccrueMonthlyInterestAddsInterest() {
        card.setDebt(BigDecimal.valueOf(1000));

        Page<CreditCard> page = new PageImpl<>(List.of(card));
        when(creditCardRepository.findAll(any(Pageable.class))).thenReturn(page);

        creditCardService.accrueMonthlyInterest();

        // 12% годовых = 1% в месяц → 10 от 1000
        assertEquals(BigDecimal.valueOf(10.00).setScale(2), card.getAccruedInterest().setScale(2));
        verify(creditCardRepository, atLeastOnce()).save(card);
    }

    /**
     * Проверяет, что при внесении суммы, большей чем проценты:
     * - проценты гасятся полностью;
     * - остаток идёт в погашение тела;
     * - баланс растёт на ту же величину, что уменьшился долг.
     */
    @Test
    void testDepositPaysOffInterestAndPartOfDebt() {
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

    /**
     * Проверяет, что при внесении суммы, меньшей чем проценты:
     * - тело долга не уменьшается;
     * - проценты уменьшаются на внесённую сумму;
     * - баланс не изменяется.
     */
    @Test
    void testDepositPaysOffPartOfInterestOnly() {
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

    /**
     * Проверяет, что при снятии 300 со счёта с балансом 1200
     * тело долга корректно обновляется.
     */
    @Test
    void testWithdrawCreatesDebtCorrectly() {
        card.setBalance(BigDecimal.valueOf(1200));

        creditCardService.processWithdraw(card, BigDecimal.valueOf(300));

        assertEquals(BigDecimal.ZERO, card.getAccruedInterest());
        assertEquals(BigDecimal.valueOf(100), card.getDebt());      // долг после снятия
        assertEquals(BigDecimal.valueOf(900), card.getBalance());   // баланс уменьшился
        assertEquals(BigDecimal.valueOf(100), card.getTotalDebt());
    }

    /**
     * Проверяет, что у нового аккаунта без долга и процентов
     * при запуске расчёта всё остаётся по нулям.
     */
    @Test
    void testAccrueInterestOnEmptyCardDoesNothing() {
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
