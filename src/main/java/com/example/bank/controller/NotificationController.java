package com.example.bank.controller;


import com.example.bank.model.NotificationResponse;
import com.example.bank.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Получить только непрочитанные уведомления пользователя
    @GetMapping("/unread")
    @Operation(summary = "Получить непрочитанные уведомления", description = "Получение списка непрочитанных уведомлений пользователя")
    public List<NotificationResponse> getUnreadNotifications() {
        return notificationService.getUnreadNotification();
    }
    //Получить все уведомления
    @GetMapping("/all")
    @Operation(summary = "Получить все уведомления", description = "Получение списка всех уведомлений пользователя")
    public List<NotificationResponse> getAllNotifications() {
        return notificationService.getAlldNotification();
    }
}
