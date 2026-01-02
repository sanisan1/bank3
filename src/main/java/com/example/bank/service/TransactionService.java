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



    // Операция пополнения счета
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#id)")
    public CardDto deposit(Long id, BigDecimal amount, String comment) {
        log.info("Deposit to card {}: amount {}", id, amount);
        try {
            Card card = cardService.getCardById(id);
            AbstractCardService service = serviceMap.get(card.getClass());
            if (service == null) {
                log.error("Unsupported card type for deposit: {}", card.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            Card updatedCard = service.processDeposit(card, amount);

            Transaction transaction = new Transaction();
            transaction.setToCard(card.getCardNumber());
            transaction.setAmount(amount);
            transaction.setType(OperationType.deposit);
            transaction.setComment(comment);
            transaction.setUser(card.getUser());
            transactionRepository.save(transaction);
            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(updatedCard);
        } catch (Exception e) {
            log.error("Deposit error for card {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // Операция снятия со счета
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#id)")
    public CardDto withdraw(Long id, BigDecimal amount, String comment) {
        log.info("Withdraw from card {}: amount {}", id, amount);
        try {
            Card card = cardService.getCardById(id);
            AbstractCardService service = serviceMap.get(card.getClass());
            if (service == null) {
                log.error("Unsupported card type for withdrawal: {}", card.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            Card updatedCard = service.processWithdraw(card, amount);

            Transaction transaction = new Transaction();
            transaction.setFromCard(card.getCardNumber());
            transaction.setAmount(amount);
            transaction.setType(OperationType.withdraw);
            transaction.setComment(comment);
            transaction.setUser(card.getUser());
            transactionRepository.save(transaction);


            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(updatedCard);
        } catch (Exception e) {
            log.error("Withdraw error for card {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    // Операция перевода средств
    @Transactional
    @PreAuthorize("@cardSecurity.isOwner(#fromId) and @cardSecurity.isOwner(#toId)")
    public CardDto transfer(Long fromId, Long toId, BigDecimal amount, String comment) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Невозможно перевести средства на тот же счёт");
        }

        log.info("Transfer: {} → {}, amount {}", fromId, toId, amount);
        try {
            Card fromCard = cardService.getCardById(fromId);
            Card toCard = cardService.getCardById(toId);

            AbstractCardService fromService = serviceMap.get(fromCard.getClass());
            AbstractCardService toService = serviceMap.get(toCard.getClass());
            if (fromService == null || toService == null) {
                log.error("Unsupported card type for transfer: {} or {}", fromCard.getClass(), toCard.getClass());
                throw new InvalidOperationException("Unsupported card type");
            }

            fromCard = fromService.processWithdraw(fromCard, amount);
            toService.processDeposit(toCard, amount);

            Transaction transaction = new Transaction();
            transaction.setFromCard(fromCard.getCardNumber());
            transaction.setToCard(toCard.getCardNumber());
            transaction.setAmount(amount);
            transaction.setType(OperationType.transfer);
            transaction.setComment(comment);
            transaction.setUser(fromCard.getUser());
            transactionRepository.save(transaction);

            eventProducer.sendTransactionEvent(NotificationMapper.toEventDTO(transaction));


            return CardMapper.toDto(fromCard);

        } catch (Exception e) {
            log.error("Transfer error from {} to {}: {}", fromId, toId, e.getMessage(), e);
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

    @PreAuthorize("hasRole('ADMIN')")
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