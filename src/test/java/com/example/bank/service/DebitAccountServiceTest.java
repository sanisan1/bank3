package com.example.bank.service;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
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
    }

    @Test
    void createCard_shouldCreateDebitCard() {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setBlocked(false);

        DebitCard card = new DebitCard();
        card.setId(1L);
        card.setCardNumber("1234567890");
        card.setUser(user);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("testUser");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(cardRepository.save(any(DebitCard.class))).thenReturn(card);

        // Act
        DebitCardResponse response = debitCardService.createCard();

        // Assert
        assertNotNull(response);
        assertEquals("1234567890", response.getCardNumber());
        verify(cardRepository, times(1)).save(any(DebitCard.class));
    }



    @Test
    void createCard_shouldThrowExceptionWhenUserNotAuthenticated() {

        when(securityContext.getAuthentication()).thenReturn(null);


        assertThrows(AccessDeniedException.class, () -> debitCardService.createCard());
        verify(cardRepository, never()).save(any(DebitCard.class));
    }


    @Test
    void testDeposit() {
        debitCardService.processDeposit(card, new BigDecimal(100));
        assertEquals(new BigDecimal(1100), card.getBalance());

    }

    @Test
    void testWithdraw() {
        debitCardService.processWithdraw(card, new BigDecimal(100));
        assertEquals(new BigDecimal(900), card.getBalance());
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenAmountIsNegative() {
        assertThrows(IllegalArgumentException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(-100)));
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenAmountIsGreaterThanBalance() {
        assertThrows(InvalidOperationException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(1100)));
    }

    @Test
    void testWithdraw_shouldThrowExceptionWhenCardIsBlocked() {
        card.setBlocked(true);
        assertThrows(CardBlockedException.class, () -> debitCardService.processWithdraw(card, new BigDecimal(100)));
    }




}