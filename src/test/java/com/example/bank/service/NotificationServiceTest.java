package com.example.bank.service;

import com.example.bank.model.Notification;

import com.example.bank.model.NotificationResponse;
import com.example.bank.model.user.User;
import com.example.bank.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    private User currentUser;
    private List<Notification> notifications;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        currentUser = new User();
        currentUser.setUserId(1L);

        notifications = new ArrayList<>();
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUserId(1L);
        notification1.setRead(false);
        notification1.setTitle("Test Notification 1");
        notification1.setMessage("This is test notification 1");

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(1L);
        notification2.setRead(false);
        notification2.setTitle("Test Notification 2");
        notification2.setMessage("This is test notification 2");

        notifications.add(notification1);
        notifications.add(notification2);
    }

    @Test
    void testGetUnreadNotification() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(notificationRepository.findByUserIdAndReadFalse(1L)).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.getUnreadNotification();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findByUserIdAndReadFalse(1L);
        verify(userService, times(1)).getCurrentUser();

        // Проверка, что уведомления помечаются как прочитанные после получения
        assertTrue(notifications.get(0).getRead());
        assertTrue(notifications.get(1).getRead());
    }

    @Test
    void testGetAlldNotification() {
        when(userService.getCurrentUser()).thenReturn(currentUser);
        when(notificationRepository.findByUserId(1L)).thenReturn(notifications);

        List<NotificationResponse> result = notificationService.getAlldNotification();

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(notificationRepository, times(1)).findByUserId(1L);
        verify(userService, times(1)).getCurrentUser();

        // Проверка, что все уведомления помечаются как прочитанные
        assertTrue(notifications.get(0).getRead());
        assertTrue(notifications.get(1).getRead());
    }
}