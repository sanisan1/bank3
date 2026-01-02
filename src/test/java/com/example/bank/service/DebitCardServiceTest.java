package com.example.bank.service;

import com.example.bank.Enums.CardStatus;
import com.example.bank.exception.CardBlockedException;
import com.example.bank.exception.InvalidOperationException;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.model.card.debitCard.DebitCardResponse;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DebitCardServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CardSecurity cardSecurity;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private DebitCardService debitCardService;

    private DebitCard card;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
        card = new DebitCard();
        card.setCardNumber("1234567890");
        card.setBalance(new BigDecimal(1000));
        card.setExpiryDate(LocalDate.now().plusYears(1));
        card.setStatus(CardStatus.ACTIVE);
    }

    @Test
    void createCard_shouldCreateDebitCard() {
        Long userId = 1L;
        User user = new User();
        user.setUserId(userId);
        user.setBlocked(false);

        DebitCard card = new DebitCard();
        card.setId(1L);
        card.setCardNumber("1234567890");
        card.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cardRepository.save(any(DebitCard.class))).thenReturn(card);

        DebitCardResponse response = debitCardService.createCard(userId);

        // Проверка, что карта создается с маскированным номером
        assertNotNull(response);
        assertEquals("**** **** **** 7890", response.getCardNumber());
        verify(cardRepository, times(1)).save(any(DebitCard.class));
    }



    @Test
    void testDeposit() {
        // Проверка, что депозит увеличивает баланс
        debitCardService.processDeposit(card, new BigDecimal(100));
        assertEquals(new BigDecimal(1100), card.getBalance());
    }

    @Test
    void testWithdraw() {
        // Проверка, что снятие уменьшает баланс
        debitCardService.processWithdraw(card, new BigDecimal(100));
        assertEquals(new BigDecimal(900), card.getBalance());
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenAmountIsNegative() {
        // Проверка, что при отрицательной сумме выбрасывается исключение
        assertThrows(IllegalArgumentException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(-100)));
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenAmountIsGreaterThanBalance() {
        // Проверка, что при превышении баланса выбрасывается исключение
        assertThrows(InvalidOperationException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(1100)));
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenCardIsBlocked() {
        // Проверка, что при заблокированной карте выбрасывается исключение
        card.setStatus(CardStatus.BLOCKED);
        assertThrows(CardBlockedException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(100)));
    }




}