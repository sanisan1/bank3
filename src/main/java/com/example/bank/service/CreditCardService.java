package com.example.bank.service;

import com.example.bank.exception.InvalidOperationException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.mapper.CreditCardMapper;
import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.model.card.creditCard.CreditCardResponseDto;
import com.example.bank.model.card.Card;
import com.example.bank.Enums.CardType;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.CreditCardRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.security.CardSecurity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

// Сервис для работы с кредитными счетами
@Service
public class CreditCardService extends AbstractCardService {
    // Базовые значения кредита
    @Value("${bank.credit.default.interest-rate}")
    private BigDecimal defaultInterestRate;

    @Value("${bank.credit.default.limit}")
    private BigDecimal defaultCreditLimit;

    @Value("${bank.credit.default.minimum-payment-rate}")
    private BigDecimal defaultMinimumPaymentRate;

    @Value("${bank.credit.default.grace-period}")
    private Integer defaultGracePeriod;


    private static final Logger log = LoggerFactory.getLogger(CreditCardService.class);

    private final CreditCardRepository creditCardRepository;
    private final TransactionRepository transactionRepository;

    // Конструктор сервиса кредитных счетов
    public CreditCardService(CardRepository cardRepository,
                                UserRepository userRepository,
                                CardSecurity cardSecurity,
                                CreditCardRepository creditCardRepository,
                                TransactionRepository transactionRepository) {
        super(cardRepository, userRepository, cardSecurity);
        this.creditCardRepository = creditCardRepository;
        this.transactionRepository = transactionRepository;
    }

    // Начисление ежемесячных процентов по всем кредитным счетам
    @Scheduled(cron = "0 0 0 1 * ?")
    public void accrueMonthlyInterest() {
        log.info("Monthly interest accrual started for all credit cards");
        Pageable pageable = PageRequest.of(0, 500);
        Page<CreditCard> cardPage;
        do {
            cardPage = creditCardRepository.findAll(pageable);
            cardPage.getContent().forEach(acc -> {
                try {
                    acc.accrueInterest();
                    acc.updateTotalDebt();
                    creditCardRepository.save(acc);
                } catch (Exception e) {
                    log.error("Error accruing interest for card {}: {}", acc.getCardNumber(), e.getMessage(), e);
                }
            });
            pageable = cardPage.nextPageable();
        } while (cardPage.hasNext());
        log.info("Monthly interest accrual finished for all credit cards");
    }

    // Создание кредитного счета администратором
    @PreAuthorize("hasRole('ADMIN')")
    public CreditCardResponseDto createCard(Long userID, BigDecimal creditLimit, BigDecimal interestRate, Integer gracePeriod) {
        log.info("Creating credit card for userID={} with limit {} and rate {}", userID, creditLimit, interestRate);
        try {
            User user = userRepository.findById(userID)
                    .orElseThrow(() -> {
                        log.error("User not found with id={}", userID);
                        return new ResourceNotFoundException("User", "id", userID);
                    });

            if (creditLimit == null || creditLimit.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Invalid credit limit: {}", creditLimit);
                throw new InvalidOperationException("Credit limit must be > 0");
            }
            if (interestRate == null || interestRate.compareTo(BigDecimal.ZERO) < 0) {
                log.error("Invalid interest rate: {}", interestRate);
                throw new InvalidOperationException("Interest rate cannot be negative");
            }

            CreditCard acc = new CreditCard();
            acc.setUser(user);
            acc.setCardNumber(generateUniqueCardNumber());
            acc.setCreditLimit(creditLimit);
            acc.setBalance(acc.getCreditLimit());
            acc.setInterestRate(interestRate);
            acc.setMinimumPaymentRate(BigDecimal.valueOf(5)); // Значение по умолчанию
            acc.setGracePeriod(gracePeriod);
            acc.setAccruedInterest(BigDecimal.ZERO);
            acc.setCardType(CardType.CREDIT);

            CreditCard saved = creditCardRepository.save(acc);
            log.info("Credit card {} created for user {}", saved.getCardNumber(), userID);
            return CreditCardMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating credit card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Создание кредитного счета обычным пользователем
    public CreditCardResponseDto createCardforUser() {
        User user = getCurrentUser();
        Long userId = user.getUserId();
        log.info("Creating credit card by user{} with limit {} and rate {}", userId, defaultCreditLimit, defaultInterestRate);
        try {

            if (!creditCardRepository.findByUserUserId(userId).isEmpty()) {
                throw new RuntimeException("User can make only 1 credit card by himself");
            }

            CreditCard acc = new CreditCard();
            acc.setUser(user);
            acc.setCardNumber(generateUniqueCardNumber());
            acc.setCreditLimit(defaultCreditLimit);
            acc.setBalance(acc.getCreditLimit());
            acc.setInterestRate(defaultInterestRate);
            acc.setMinimumPaymentRate(defaultMinimumPaymentRate);
            acc.setGracePeriod(defaultGracePeriod);
            acc.setAccruedInterest(BigDecimal.ZERO);
            acc.setCardType(CardType.CREDIT);

            CreditCard saved = creditCardRepository.save(acc);
            log.info("Credit card {} created for user {}", saved.getCardNumber(), userId);
            return CreditCardMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Error creating credit card: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Обработка операции пополнения кредитного счета
    @Override
    public Card processDeposit(Card card, BigDecimal amount) {
        log.info("Depositing to credit card {} amount {}", card.getCardNumber(), amount);
        try {
            CreditCard acc = (CreditCard) card;
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Deposit attempt with invalid amount: {}", amount);
                throw new InvalidOperationException("Amount must be greater than zero");
            }
            checkCardBlock(acc);

            BigDecimal toInterest = acc.getAccruedInterest().min(amount);
            acc.setAccruedInterest(acc.getAccruedInterest().subtract(toInterest));
            BigDecimal left = amount.subtract(toInterest);

            if (left.compareTo(BigDecimal.ZERO) > 0) {
                acc.setBalance(acc.getBalance().add(left));
                acc.setDebt(acc.getDebt().subtract(left));
            }
            acc.updateTotalDebt();
            return acc;
        } catch (Exception e) {
            log.error("Error depositing to credit card {}: {}", card.getCardNumber(), e.getMessage(), e);
            throw e;
        }
    }

    // Обработка операции снятия с кредитного счета
    @Override
    public Card processWithdraw(Card card, BigDecimal amount) {
        log.info("Withdrawing from credit card {} amount {}", card.getCardNumber(), amount);
        try {
            CreditCard acc = (CreditCard) card;
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Withdraw attempt with invalid amount: {}", amount);
                throw new InvalidOperationException("Amount must be greater than zero");
            }
            checkCardBlock(acc);

            BigDecimal balance = card.getBalance();
            if (amount.compareTo(balance) > 0) {
                log.error("Insufficient funds: card {}, balance {}, withdrawal attempt {}", card.getCardNumber(), balance, amount);
                throw new InvalidOperationException("Exceeds available credit");
            }
            BigDecimal creditLimit = acc.getCreditLimit();
            balance = balance.subtract(amount);

            BigDecimal totalDebt = acc.getDebt();
            BigDecimal availableOwnFunds = creditLimit.subtract(totalDebt);
            if (balance.compareTo(availableOwnFunds) < 0) {
                BigDecimal newDebt = availableOwnFunds.subtract(balance);
                totalDebt = totalDebt.add(newDebt);
            }

            acc.setBalance(balance);
            acc.setDebt(totalDebt);
            acc.updateTotalDebt();

            return acc;
        } catch (Exception e) {
            log.error("Error withdrawing from credit card {}: {}", card.getCardNumber(), e.getMessage(), e);
            throw e;
        }
    }

    // Удаление кредитного счета по номеру (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteByCardNumber(String cardNumber) {
        log.info("Deleting credit card: {}", cardNumber);
        try {
            CreditCard acc = (CreditCard) getCardByNumber(cardNumber);
            acc.updateTotalDebt();
            if (acc.hasDebt()) {
                log.error("Delete attempt for card {} with outstanding debt", cardNumber);
                throw new InvalidOperationException("Cannot delete credit card with outstanding debt");
            }
            cardRepository.deleteByCardNumber(cardNumber);
            log.info("Credit card {} successfully deleted", cardNumber);
        } catch (Exception e) {
            log.error("Error deleting credit card {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Увеличение кредитного лимита (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public CreditCard increaseCreditLimit(String cardNumber, BigDecimal newLimit) {
        log.info("Increasing credit limit for card {} to {}", cardNumber, newLimit);
        try {
            CreditCard acc = (CreditCard) getCardByNumber(cardNumber);
            if (newLimit == null || newLimit.compareTo(acc.getCreditLimit()) <= 0) {
                log.error("Invalid new limit {} (current limit = {})", newLimit, acc.getCreditLimit());
                throw new InvalidOperationException("New limit must be greater than current limit");
            }

            BigDecimal delta = newLimit.subtract(acc.getCreditLimit());
            acc.setCreditLimit(newLimit);
            acc.setBalance(acc.getBalance().add(delta));
            acc.updateTotalDebt();

            return acc;
        } catch (Exception e) {
            log.error("Error increasing limit {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Уменьшение кредитного лимита (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public CreditCard decreaseCreditLimit(String cardNumber, BigDecimal newLimit) {
        log.info("Decreasing credit limit for card {} to {}", cardNumber, newLimit);
        try {
            CreditCard acc = (CreditCard) getCardByNumber(cardNumber);
            if (newLimit == null || newLimit.compareTo(BigDecimal.ZERO) <= 0) {
                log.error("Attempt to set invalid limit: {}", newLimit);
                throw new InvalidOperationException("New limit must be > 0");
            }
            if (newLimit.compareTo(acc.getBalance()) < 0) {
                log.error("Attempt to set limit below balance: limit={}, balance={}", newLimit, acc.getBalance());
                throw new InvalidOperationException("New limit cannot be less than current available balance");
            }

            acc.setCreditLimit(newLimit);
            acc.updateTotalDebt();
            return acc;
        } catch (Exception e) {
            log.error("Error decreasing limit {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Установка процентной ставки (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public CreditCard setInterestRate(String cardNumber, BigDecimal newRate) {
        log.info("Setting new interest rate for card {}: {}", cardNumber, newRate);
        try {
            if (newRate == null || newRate.compareTo(BigDecimal.ZERO) < 0) {
                log.error("Attempt to set negative interest rate: {}", newRate);
                throw new InvalidOperationException("Interest rate cannot be negative");
            }

            CreditCard acc = (CreditCard) getCardByNumber(cardNumber);
            acc.setInterestRate(newRate);
            return acc;
        } catch (Exception e) {
            log.error("Error setting interest rate for card {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }
}