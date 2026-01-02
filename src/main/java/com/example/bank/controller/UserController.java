package com.example.bank.controller;

import com.example.bank.mapper.UserMapper;
import com.example.bank.model.user.CreateUserDto;
import com.example.bank.model.user.User;
import com.example.bank.model.user.UserDto;
import com.example.bank.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "Создать пользователя", description = "Создание нового пользователя в системе")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserDto createUserDto) {
        UserDto createdUser = userService.createUser(createUserDto);
        return ResponseEntity.status(201).body(createdUser);
    }

    @GetMapping
    @Operation(summary = "Получить всех пользователей", description = "Получение списка всех пользователей системы")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID", description = "Получение пользователя по его ID")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Обновить пользователя", description = "Частичное обновление данных пользователя")
    public ResponseEntity<UserDto> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> updates) {
        User user = userService.getUserById(id); // если не найдёт — GlobalExceptionHandler вернёт 404

        updates.forEach((key, value) -> {
            switch (key) {
                case "username" -> user.setUsername((String) value);
                case "password" -> user.setPassword((String) value);
                case "email" -> user.setEmail((String) value);
                case "firstName" -> user.setFirstName((String) value);
                case "lastName" -> user.setLastName((String) value);
                case "phoneNumber" -> user.setPhoneNumber((String) value);
                case "status" -> user.setBlocked((Boolean) value);
            }
        });

        User updatedUser = userService.update(user);
        return ResponseEntity.ok(UserMapper.toDto(updatedUser));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя", description = "Удаление пользователя по ID")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUserById(id);
        return ResponseEntity.noContent().build();
    }
}
