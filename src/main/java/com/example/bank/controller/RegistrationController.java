package com.example.bank.controller;

import com.example.bank.model.user.CreateUserDto;
import com.example.bank.Enums.Role;
import com.example.bank.model.user.User;
import com.example.bank.model.user.UserDto;
import com.example.bank.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RegistrationController {
    private final UserService userService;
    public RegistrationController(UserService userService) {
        this.userService = userService;
    }
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody CreateUserDto createUserDto) {

        createUserDto.setRole(Role.valueOf("USER"));
        return ResponseEntity.ok(userService.createUser(createUserDto));
    }


}
