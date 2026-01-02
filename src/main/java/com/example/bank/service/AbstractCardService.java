package com.example.bank.service;

import com.example.bank.Enums.CardStatus;
import com.example.bank.exception.CardBlockedException;
import com.example.bank.exception.InvalidOperationException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.model.card.Card;
import com.example.bank.Enums.Role;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;

// Абстрактный сервис для управления банковскими счетами
@Transactional
public abstract class AbstractCardService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    protected final CardRepository cardRepository;
    protected final UserRepository userRepository;
    protected final CardSecurity cardSecurity;
    private final SecureRandom secureRandom = new SecureRandom();

    public AbstractCardService(CardRepository cardRepository,
                                  UserRepository userRepository,
                                  CardSecurity cardSecurity) {
        this.cardRepository = cardRepository;
        this.userRepository = userRepository;
        this.cardSecurity = cardSecurity;
    }

    // Получает текущего аутентифицированного пользователя

    protected User getCurrentUser() {
        log.info("Retrieving current user from security context");
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
                log.error("Unauthenticated access attempt");
                throw new AccessDeniedException("User is not authenticated");
            }
            String username = authentication.getName();
            return userRepository.findByUsername(username)
                    .orElseThrow(() -> {
                        log.error("User not found with username={}", username);
                        return new ResourceNotFoundException("User", "username", username);
                    });
        } catch (Exception e) {
            log.error("Error retrieving user: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Проверяет, заблокирован ли счет
    protected void checkCardBlock(Card card) {
        if (card == null) {
            log.error("Card argument is null!");
            throw new IllegalArgumentException("Счет не может быть null");
        }
        if (card.getStatus() == CardStatus.BLOCKED) {
            log.error("Operation attempt on status card {}", card.getCardNumber());
            throw new CardBlockedException(card);
        }
        if (card.getStatus() == CardStatus.CLOSED) {
            log.error("Operation attempt on status card {}", card.getCardNumber());
            throw new CardBlockedException(card);
        }
        if (LocalDate.now().isAfter(card.getExpiryDate())) {
            log.error("Operation attempt on status card {}", card.getCardNumber());
            throw new CardBlockedException(card);
        }//////////////////////
    }


// Генерирует уникальный 16-значный номер карты
    public String generateUniqueCardNumber() {
        log.info("Generating unique card number");
        String number;

        long min = 1_000_000_000_000_000L;  // 16 цифр, первая минимум 1
        long max = 9_999_999_999_999_999L;  // 16 цифр, первая максимум 9

        try {
            do {
                long randomNum = Math.abs(secureRandom.nextLong());
                long valueInRange = min + (randomNum % (max - min + 1)); // [min, max]
                number = Long.toString(valueInRange);
            } while (cardRepository.existsByCardNumber(number));

            return number;
        } catch (Exception e) {
            log.error("Error generating card number: {}", e.getMessage(), e);
            throw e;
        }
    }



    // Находит счет по его номеру
    public Card getCardByNumber(String cardNumber) {
        log.info("Searching card by number: {}", cardNumber);
        try {
            return cardRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> {
                        log.error("Card not found with number {}", cardNumber);
                        return new ResourceNotFoundException("Card", "cardNumber", cardNumber);
                    });
        } catch (Exception e) {
            log.error("Error searching card by number: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Возвращает все счета пользователя или все счета для администратора
    public List<Card> findAll(User currentUser) {
        log.info("Retrieving cards list for user: {}", currentUser.getUsername());
        try {
            if (currentUser.getRole() == Role.ADMIN) {
                return cardRepository.findAll();
            } else {
                return cardRepository.findByUserUserId(currentUser.getUserId());
            }
        } catch (Exception e) {
            log.error("Error retrieving cards list: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Находит счет по ID
    public Card findById(Long id) {
        log.info("Searching card by ID: {}", id);
        try {
            return cardRepository.findById(id)
                    .orElseThrow(() -> {
                        log.error("Card not found with ID {}", id);
                        return new ResourceNotFoundException("Card", "id", id);
                    });
        } catch (Exception e) {
            log.error("Error searching card by ID: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Обновляет информацию о счете
    public Card update(Card card) {
        log.info("Updating card with ID: {}", card.getCardNumber());
        try {
            return cardRepository.save(card);
        } catch (Exception e) {
            log.error("Error updating card: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deleteById(Long id) {
        log.info("Deleting card by ID: {}", id);
        try {
            cardRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting card by ID: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Блокирует счет (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Card blockCard(Long id) {
        log.info("Admin blocking card: {}", id);
        try {
            Card card = getCardById(id);
            card.setStatus(CardStatus.BLOCKED);
            return cardRepository.save(card);
        } catch (Exception e) {
            log.error("Error while blocking card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Разблокирует счет (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Card unblockCard(Long id) {
        log.info("Admin unblocking card: {}", id);
        try {
            Card card = getCardById(id);
            card.setStatus(CardStatus.ACTIVE);
            return cardRepository.save(card);
        } catch (Exception e) {
            log.error("Error while unblocking card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Получает счет по ID
    public Card getCardById(Long cardId) {
        try {
            return cardRepository.findById(cardId)
                    .orElseThrow(() -> {
                        log.error("Card not found with ID {}", cardId);
                        return new ResourceNotFoundException("Card", "id", cardId);
                    });
        } catch (Exception e) {
            log.error("Error getting card by ID: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Обрабатывает операцию пополнения счета
    protected Card processDeposit(Card card, BigDecimal amount) {
        log.info("Depositing to card {} amount {}", card.getCardNumber(), amount);
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Deposit attempt with non-positive amount: {}", amount);
                throw new IllegalArgumentException("Сумма пополнения должна быть больше нуля");
            }
            checkCardBlock(card);

            card.setBalance(card.getBalance().add(amount));
            return card;
        } catch (Exception e) {
            log.error("Error depositing to card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Обрабатывает операцию снятия со счета
    protected Card processWithdraw(Card card, BigDecimal amount) {
        log.info("Withdrawing from card {} amount {}", card.getCardNumber(), amount);
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Withdrawal attempt with non-positive amount: {}", amount);
                throw new IllegalArgumentException("Сумма снятия должна быть больше нуля");
            }
            checkCardBlock(card);
            BigDecimal balance = card.getBalance();
            if (amount.compareTo(balance) > 0) {
                log.error("Insufficient funds: card {}, balance {}, attempt to withdraw {}", card.getCardNumber(), balance, amount);
                throw new InvalidOperationException("Недостаточно средств");
            }
            card.setBalance(balance.subtract(amount));
            return card;
        } catch (Exception e) {
            log.error("Error withdrawing from card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Удаляет счет пользователем только если баланс равен нулю
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteCard(Long id) {
        log.info("User deleting card: {}", id);
        try {

            Card card = getCardById(id);
            checkCardBlock(card);


            if (card.getBalance().compareTo(BigDecimal.ZERO) != 0) {
                log.error("Attempt to delete card with non-zero balance: {}, balance={}", card.getCardNumber(), card.getBalance());
                throw new InvalidOperationException("Невозможно удалить счет с ненулевым балансом");
            }

            cardRepository.deleteById(id);
        } catch (Exception e) {
            log.error("Error deleting card by user: {}", e.getMessage(), e);
            throw e;
        }
    }
}