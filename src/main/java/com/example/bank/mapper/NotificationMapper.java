package com.example.bank.mapper;

import com.example.bank.Enums.OperationType;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.kafka.EventDTO;
import com.example.bank.Enums.NotflicationType;
import com.example.bank.model.Notification;
import com.example.bank.model.NotificationResponse;
import com.example.bank.model.transaction.Transaction;
import com.example.bank.repository.CardRepository;
import com.example.bank.service.CardServiceImpl;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    private static CardRepository cardRepository;

    public NotificationMapper(CardRepository cardRepository) {
        NotificationMapper.cardRepository = cardRepository;
    }


    public static NotflicationType toNotificationType(OperationType opType) {
        switch (opType) {
            case deposit:
                return NotflicationType.DEPOSIT;
            case withdraw:
                return NotflicationType.WITHDRAW;
            case transfer:
                return NotflicationType.TRANSFER;
            case payment:
                return NotflicationType.PAYMENT;
            default:
                return NotflicationType.INFO; // fallback, если вдруг появится новый тип
        }
    }
    public static EventDTO toEventDTO(Transaction transaction) {
        EventDTO eventDTO = new EventDTO();
        eventDTO.setType(transaction.getType());

        //Если мы депозитим значит счёт с которым проводится операция пользавталем to сли перевод снять from
        if (transaction.getType() == OperationType.deposit) {
            eventDTO.setCardNumber(transaction.getToCard());
        } else {
            eventDTO.setCardNumber(transaction.getFromCard());
        }

        eventDTO.setCardTransferTo(CardServiceImpl.maskCardNumber(transaction.getToCard()));
        eventDTO.setAmount(transaction.getAmount());
        eventDTO.setUserId(transaction.getUser().getUserId());
        eventDTO.setComment(transaction.getComment());
        eventDTO.setTransactionId(transaction.getId());
        return eventDTO;
    }
    public static Notification toNotification(EventDTO eventDTO) {
        Notification notification = new Notification();
        NotflicationType type = toNotificationType(eventDTO.getType());
        notification.setUserId(eventDTO.getUserId());
        notification.setType(type);
        if (type == NotflicationType.TRANSFER) {
            String CardTransferTo = eventDTO.getCardTransferTo();
            notification.setCardTransferTo(CardTransferTo);
        }
        notification.setCardNumber(eventDTO.getCardNumber());
        notification.setAmount(eventDTO.getAmount());
        notification.setComment(eventDTO.getComment());
        notification.setTitle(toTitle(type));
        notification.setReferenceId(eventDTO.getTransactionId());
        notification.setMessage(toMessage(eventDTO, type));

        return notification;
    }

    public static Notification makeNotificationForReciever(Notification notification) {
        Notification clone = new Notification();
        String accTrTo = notification.getCardTransferTo();
        clone.setUserId(cardRepository.findByCardNumber(accTrTo)
                .orElseThrow(() -> new ResourceNotFoundException("Card", "UserId",accTrTo))
                .getUser().getUserId());
        clone.setType(notification.getType());
        clone.setTitle(notification.getTitle());
        clone.setCardNumber(notification.getCardNumber());
        clone.setCardTransferTo(accTrTo);
        clone.setAmount(notification.getAmount());
        clone.setComment(notification.getComment());
        clone.setMessage(notification.getMessage());
        clone.setCreatedAt(notification.getCreatedAt());// новые всегда непрочитанные
        clone.setReferenceId(notification.getReferenceId());
        // ... и другие нужные поля
        return clone;
    }

    public static NotificationResponse toResponse(Notification notification) {
        NotificationResponse response = new NotificationResponse();
        response.setType(notification.getType());
        response.setTitle(notification.getTitle());
        response.setCardTransferTo(notification.getCardTransferTo());
        // Маскируем номер только для TRANSFER
        if (notification.getType() == NotflicationType.TRANSFER) {
            response.setCardNumber(maskCard(notification.getCardNumber()));
        } else {
            response.setCardNumber(notification.getCardNumber());
        }
        response.setComment(notification.getComment());
        response.setMessage(notification.getMessage());
        response.setRead(notification.getRead());
        response.setCreatedAt(notification.getCreatedAt());
        response.setAmount(notification.getAmount());
        response.setReferenceId(notification.getReferenceId());
        return response;
    }


    public static String toTitle(NotflicationType type) {
        switch (type) {
            case DEPOSIT:
                return "Зачисление";
            case WITHDRAW:
                return "Снятие";
            case TRANSFER:
                return "Перевод";
            case FRAUD:
                return "Подозрительная операция";
            case INFO:
                return "Информация";
            default:
                return "";
        }
    }
    public static String toMessage(EventDTO eventDTO, NotflicationType type) {
        String cardPart = "счёт: " + maskCard(eventDTO.getCardNumber());
        String amountPart = (eventDTO.getAmount() != null ? " на сумму " + eventDTO.getAmount() + " ₽" : "");
        String commentPart = (eventDTO.getComment() != null && !eventDTO.getComment().isEmpty())
                ? " (Комментарий: " + eventDTO.getComment() + ")"
                : "";


        switch (type) {
            case DEPOSIT:
                return "Пополнение " + cardPart + amountPart + commentPart;
            case WITHDRAW:
                return "Снятие средств с " + cardPart + amountPart + commentPart;
            case TRANSFER:
                String cardTransferToPart = "на счёт " + maskCard(eventDTO.getCardTransferTo());
                return "Перевод с счёта " + cardPart + amountPart + cardTransferToPart + commentPart;
            case FRAUD:
                return "Обнаружена подозрительная активность по вашему счёту. Срочно свяжитесь с банком!" + commentPart;
            case INFO:
            default:
                return commentPart.isEmpty() ? "Информационное уведомление" : commentPart;
        }
    }

    // Можно добавить маску для отображения номера счёта (****7071)
    private static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) return cardNumber == null ? "" : cardNumber;
        return "****" + cardNumber.substring(cardNumber.length() - 4);
    }
}
