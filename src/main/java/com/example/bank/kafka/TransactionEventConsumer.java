package com.example.bank.kafka;


import com.example.bank.mapper.NotificationMapper;
import com.example.bank.Enums.NotflicationType;
import com.example.bank.model.Notification;
import com.example.bank.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Service
public class TransactionEventConsumer {

    private final NotificationRepository notificationRepository;



    private final ObjectMapper objectMapper;

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
;

    public TransactionEventConsumer(NotificationRepository notificationRepository, ObjectMapper objectMapper) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        log.info(">>> TransactionEventConsumer constructed");
    }

    @KafkaListener(topics = "TransactionEvent", groupId = "hello-group")
    public void listen(String message) {
        try {

            EventDTO event = objectMapper.readValue(message, EventDTO.class);

            log.info("💡 [KAFKA] Получено событие: type={}, card={}, amount={}",
                   event.getType(), event.getCardNumber(), event.getAmount());
            Notification notification = NotificationMapper.toNotification(event);
            log.info("notification.getMessage() = " + notification.getMessage());
            notificationRepository.save(notification);
            if (notification.getType() == NotflicationType.TRANSFER) {
                notificationRepository.save(NotificationMapper.makeNotificationForReciever(notification));

            }


            // Можно дальше обработать (записать уведомление и т.д.)
        } catch (Exception ex) {
            log.error("Ошибка парсинга события из Kafka: {}", message, ex);
        }
    }






}
