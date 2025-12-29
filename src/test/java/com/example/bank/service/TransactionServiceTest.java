package com.example.bank.service;

import com.example.bank.Enums.OperationType;
import com.example.bank.exception.InvalidOperationException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.kafka.TransactionEventProducer;
import com.example.bank.model.card.CardDto;
import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.model.transaction.Transaction;
import com.example.bank.model.transaction.TransactionResponse;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CreditCardService creditCardService;
    @Mock private DebitCardService debitCardService;
    @Mock private TransactionEventProducer eventProducer;

    @InjectMocks
    private TransactionService transactionService;

    private CreditCard creditCard;
    private DebitCard debitCard;

    @BeforeEach
    void setUp() {
        creditCard = new CreditCard();
        creditCard.setCardNumber("CR123");
        creditCard.setBalance(BigDecimal.valueOf(1000));
        creditCard.setExpiryDate(LocalDate.now().plusYears(1));
        creditCard.setStatus(com.example.bank.Enums.CardStatus.ACTIVE);

        debitCard = new DebitCard();
        debitCard.setCardNumber("DB123");
        debitCard.setBalance(BigDecimal.valueOf(500));
        debitCard.setExpiryDate(LocalDate.now().plusYears(1));
        debitCard.setStatus(com.example.bank.Enums.CardStatus.ACTIVE);
    }

    @Test
    void deposit_ShouldWorkForCreditCard() {
        BigDecimal amount = BigDecimal.valueOf(100);
        CreditCard updated = new CreditCard();
        updated.setCardNumber("CR123");
        updated.setBalance(BigDecimal.valueOf(1100));
        // И user!
        User user = new User();
        user.setUserId(123L);
        creditCard.setUser(user);
        updated.setUser(user); // и для updated, чтобы CardMapper не упал

        when(cardRepository.findByCardNumber("CR123")).thenReturn(Optional.of(creditCard));
        when(creditCardService.processDeposit(eq(creditCard), eq(amount))).thenReturn(updated);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CardDto result = transactionService.deposit("CR123", amount, "ok");
        assertEquals(updated.getBalance(), result.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventProducer).sendTransactionEvent(any());
    }


    @Test
    void deposit_ShouldThrowForNegativeAmount_Credit() {
        BigDecimal negative = BigDecimal.valueOf(-100);
        when(cardRepository.findByCardNumber("CR123")).thenReturn(Optional.of(creditCard));
        when(creditCardService.processDeposit(eq(creditCard), eq(negative)))
                .thenThrow(new InvalidOperationException("Amount must be greater than zero"));

        InvalidOperationException ex = assertThrows(InvalidOperationException.class,
                () -> transactionService.deposit("CR123", negative, "invalid"));
        assertEquals("Amount must be greater than zero", ex.getMessage());
        verify(transactionRepository, never()).save(any());
        verify(eventProducer, never()).sendTransactionEvent(any());
    }

    @Test
    void deposit_ShouldThrowIfCardNotFound() {
        when(cardRepository.findByCardNumber("XXX")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.deposit("XXX", BigDecimal.TEN, "no"));
        verify(transactionRepository, never()).save(any());
        verify(eventProducer, never()).sendTransactionEvent(any());
    }

    @Test
    void withdraw_ShouldWorkForDebitCard() {
        BigDecimal amount = BigDecimal.valueOf(50);
        DebitCard updated = new DebitCard();
        updated.setCardNumber("DB123");
        updated.setBalance(BigDecimal.valueOf(450));
        User user = new User();
        user.setUserId(12L);
        updated.setUser(user);
        // ВАЖНО! Вот эта строка нужна:
        debitCard.setUser(user);

        when(cardRepository.findByCardNumber("DB123")).thenReturn(Optional.of(debitCard));
        when(debitCardService.processWithdraw(eq(debitCard), eq(amount))).thenReturn(updated);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CardDto result = transactionService.withdraw("DB123", amount, "ok");
        assertEquals(updated.getBalance(), result.getBalance());
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventProducer).sendTransactionEvent(any());
    }


    @Test
    void withdraw_ShouldThrowForNegativeAmount_Debit() {
        BigDecimal negative = BigDecimal.valueOf(-100);
        when(cardRepository.findByCardNumber("DB123")).thenReturn(Optional.of(debitCard));
        when(debitCardService.processWithdraw(eq(debitCard), eq(negative)))
                .thenThrow(new InvalidOperationException("Amount must be greater than zero"));

        assertThrows(InvalidOperationException.class,
                () -> transactionService.withdraw("DB123", negative, "invalid"));
        verify(transactionRepository, never()).save(any());
        verify(eventProducer, never()).sendTransactionEvent(any());
    }

    @Test
    void transfer_ShouldWork_FromDebitToCredit() {
        BigDecimal amount = BigDecimal.valueOf(100);

        DebitCard from = new DebitCard();
        from.setCardNumber("DB12");
        from.setBalance(BigDecimal.valueOf(600));
        User user = new User();
        user.setUserId(12L);
        from.setUser(user);

        CreditCard to = new CreditCard();
        to.setCardNumber("CR1");
        to.setBalance(BigDecimal.valueOf(200));

        when(cardRepository.findByCardNumber("DB12")).thenReturn(Optional.of(from));
        when(cardRepository.findByCardNumber("CR1")).thenReturn(Optional.of(to));
        when(debitCardService.processWithdraw(eq(from), eq(amount))).thenReturn(from);
        when(creditCardService.processDeposit(eq(to), eq(amount))).thenReturn(to);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        CardDto result = transactionService.transfer("DB12", "CR1", amount, "ok");

        assertEquals("DB12", result.getCardNumber());
        verify(transactionRepository).save(any(Transaction.class));
        verify(eventProducer).sendTransactionEvent(any());
    }

    @Test
    void transfer_ShouldThrowForNegativeAmount() {
        BigDecimal negative = BigDecimal.valueOf(-100);
        DebitCard from = new DebitCard(); from.setCardNumber("DB1");
        CreditCard to = new CreditCard(); to.setCardNumber("CR1");

        when(cardRepository.findByCardNumber("DB1")).thenReturn(Optional.of(from));
        when(cardRepository.findByCardNumber("CR1")).thenReturn(Optional.of(to));
        when(debitCardService.processWithdraw(eq(from), eq(negative)))
                .thenThrow(new InvalidOperationException("Amount must be greater than zero"));

        assertThrows(InvalidOperationException.class,
                () -> transactionService.transfer("DB1", "CR1", negative, "fail"));
        verify(transactionRepository, never()).save(any());
        verify(eventProducer, never()).sendTransactionEvent(any());
    }

    @Test
    void getTransactionById_ShouldReturnDto() {
        Transaction t = new Transaction();
        t.setAmount(BigDecimal.TEN);
        t.setType(OperationType.deposit);
        when(transactionRepository.findById(1L)).thenReturn(Optional.of(t));

        TransactionResponse dto = transactionService.getTransactionById(1L);
        assertEquals(BigDecimal.TEN, dto.getAmount());
    }

    @Test
    void getTransactionById_ShouldThrowIfNotFound() {
        when(transactionRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> transactionService.getTransactionById(99L));
    }

    @Test
    void getAllTransactions_ShouldReturnList() {
        Transaction t1 = new Transaction();
        Transaction t2 = new Transaction();
        when(transactionRepository.findAll()).thenReturn(List.of(t1, t2));

        List<TransactionResponse> result = transactionService.getAllTransactions();
        assertEquals(2, result.size());
    }

    @Test
    void getTransactionsByCard_ShouldReturnFilteredList() {
        Transaction t = new Transaction();
        t.setFromCard("DB123");
        t.setAmount(BigDecimal.TEN);
        when(transactionRepository.findByFromCardOrToCard("DB123", "DB123"))
                .thenReturn(List.of(t));

        List<TransactionResponse> result = transactionService.getTransactionsByCard("DB123");
        assertEquals(1, result.size());
    }

    @Test
    void getTransactionsByUser_ShouldReturnCombinedList() {
        // Реализуй аналогично — только моки в самом тесте!
    }

    @Test
    void getTransactionsByUser_ShouldReturnEmptyIfNoCards() {
        when(cardRepository.findByUserUserId(2L)).thenReturn(List.of());
        List<TransactionResponse> result = transactionService.getTransactionsByUser(2L);
        assertTrue(result.isEmpty());
        verify(transactionRepository, never()).findByFromCardInOrToCardIn(anyList(), anyList());
    }
}
