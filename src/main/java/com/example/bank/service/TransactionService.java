package com.example.bank.service;

import com.example.bank.Enums.OperationType;
import com.example.bank.exception.InvalidOperationException;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.kafka.TransactionEventProducer;
import com.example.bank.mapper.CardMapper;
import com.example.bank.mapper.NotificationMapper;
import com.example.bank.mapper.TransactionMapper;
import com.example.bank.model.card.Card;
import com.example.bank.model.card.CardDto;
import com.example.bank.model.card.creditCard.CreditCard;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.model.transaction.Transaction;
import com.example.bank.model.transaction.TransactionResponse;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);


    private final TransactionEventProducer eventProducer;

    private final CardRepository cardRepository;
    private final TransactionRepository transactionRepository;
    private final Map<Class<? extends Card>, AbstractCardService> serviceMap;
    private final CardServiceImpl cardService;


    public TransactionService(
            CardRepository cardRepository,
            TransactionRepository transactionRepository,
            CreditCardService creditCardService,
            DebitCardService debitCardService,
            TransactionEventProducer eventProducer, CardServiceImpl cardService   // <---- обязательно!
          ) {
        this.cardRepository = cardRepository;
        this.transactionRepository = transactionRepository;
        this.eventProducer = eventProducer;
        this.cardService = cardService;
        this.serviceMap = new HashMap<>();
        this.serviceMap.put(CreditCard.class, creditCardService);
        this.serviceMap.put(DebitCard.class, debitCardService);
    }


    private Card getCardByNumber(String cardNumber) {
        return cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found", "CardNumber", cardNumber));
    }

    // Операция пополнения счета
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#cardNumber)")
    public CardDto deposit(String cardNumber, BigDecimal amount, String comment) {
        log.info("Deposit to card {}: amount {}", cardNumber, amount);
        try {
            Card acc = getCardByNumber(cardNumber);
            AbstractCardService service = serviceMap.get(acc.getClass());
            if (service == null) {
                log.error("Unsupported card type for deposit: {}", acc.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            Card updatedCard = service.processDeposit(acc, amount);

            Transaction transaction = new Transaction();
            transaction.setToCard(cardNumber);
            transaction.setAmount(amount);
            transaction.setType(OperationType.deposit);
            transaction.setComment(comment);
            transaction.setUser(acc.getUser());
            transactionRepository.save(transaction);
            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(updatedCard);
        } catch (Exception e) {
            log.error("Deposit error for card {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Операция снятия со счета
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#cardNumber)")
    public CardDto withdraw(String cardNumber, BigDecimal amount, String comment) {
        log.info("Withdraw from card {}: amount {}", cardNumber, amount);
        try {
            Card acc = getCardByNumber(cardNumber);
            AbstractCardService service = serviceMap.get(acc.getClass());
            if (service == null) {
                log.error("Unsupported card type for withdrawal: {}", acc.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            Card updatedCard = service.processWithdraw(acc, amount);

            Transaction transaction = new Transaction();
            transaction.setFromCard(acc.getCardNumber());
            transaction.setAmount(amount);
            transaction.setType(OperationType.withdraw);
            transaction.setComment(comment);
            transaction.setUser(acc.getUser());
            transactionRepository.save(transaction);


            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(updatedCard);
        } catch (Exception e) {
            log.error("Withdraw error for card {}: {}", cardNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Операция перевода средств
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#fromNumber)")
    public CardDto transfer(String fromNumber, String toNumber, BigDecimal amount, String comment) {
        if (fromNumber.equals(toNumber)) {
            throw new IllegalArgumentException("Невозможно перевести средства на тот же счёт");
        }

        log.info("Transfer: {} → {}, amount {}", fromNumber, toNumber, amount);
        try {
            Card fromAcc = getCardByNumber(fromNumber);
            Card toAcc = getCardByNumber(toNumber);

            AbstractCardService fromService = serviceMap.get(fromAcc.getClass());
            AbstractCardService toService = serviceMap.get(toAcc.getClass());
            if (fromService == null || toService == null) {
                log.error("Unsupported card type for transfer: {} or {}", fromAcc.getClass(), toAcc.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            fromAcc = fromService.processWithdraw(fromAcc, amount);
            toService.processDeposit(toAcc, amount);

            Transaction transaction = new Transaction();
            transaction.setFromCard(fromNumber);
            transaction.setToCard(toNumber);
            transaction.setAmount(amount);
            transaction.setType(OperationType.transfer);
            transaction.setComment(comment);
            transaction.setUser(fromAcc.getUser());
            transactionRepository.save(transaction);

            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(fromAcc);

        } catch (Exception e) {
            log.error("Transfer error from {} to {}: {}", fromNumber, toNumber, e.getMessage(), e);
            throw e;
        }
    }

    // Получить транзакцию по ID
    public TransactionResponse getTransactionById(Long id) {
        return TransactionMapper.toDto(
                transactionRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", id))
        );
    }

    // Получить все транзакции
    @PreAuthorize("hasRole('ADMIN')")
    public List<TransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .map(TransactionMapper::toDto)
                .collect(Collectors.toList());
    }

    // Получить транзакции по счету
    @PreAuthorize("@cardSecurity.isOwner(#cardNumber)")
    public List<TransactionResponse> getTransactionsByCard(String cardNumber) {
        List<Transaction> transactions = transactionRepository.findByFromCardOrToCard(cardNumber, cardNumber);
        return transactions.stream()
                .map(TransactionMapper::toDto)
                .collect(Collectors.toList());
    }

    // Получить транзакции по пользователю
    public List<TransactionResponse> getTransactionsByUser(Long userId) {
        List<Card> userCards = cardRepository.findByUserUserId(userId);
        List<String> cardNumbers = userCards.stream()
                .map(Card::getCardNumber)
                .collect(Collectors.toList());

        if (cardNumbers.isEmpty()) {
            return List.of();
        }

        List<Transaction> transactions = transactionRepository.findByFromCardInOrToCardIn(
                cardNumbers,
                cardNumbers
        );

        return transactions.stream()
                .map(TransactionMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<TransactionResponse> getTransactionsForUser() {
        return getTransactionsByUser(cardService.getCurrentUser().getUserId());
    }





}