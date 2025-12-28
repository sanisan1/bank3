package com.example.bank.service;

import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.mapper.CardMapper;
import com.example.bank.mapper.UserMapper;
import com.example.bank.model.card.CardDto;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.Enums.CardType;
import com.example.bank.model.user.CreateUserDto;
import com.example.bank.model.user.User;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CardRepository cardRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, CardRepository cardRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.cardRepository = cardRepository;
    }

    // Создание нового пользователя
    public User createUser(CreateUserDto createUserDto) {
        log.info("Creating new user: username={}", createUserDto.getUsername());

        if (createUserDto.getBlocked() == null) {
            createUserDto.setBlocked(false);
        }

        User user = UserMapper.toEntity(createUserDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        try {
            User savedUser = userRepository.save(user);

            log.info("User created successfully: userId={}, username={}, blocked={}",
                    savedUser.getUserId(), savedUser.getUsername(), savedUser.getBlocked());

            return savedUser;
        } catch (DataIntegrityViolationException e) {
            log.warn("Failed to create user due to constraint violation: username={}", createUserDto.getUsername());
            throw new DataIntegrityViolationException("User with this username already exists", e);
        }
    }

    // Получение списка всех пользователей (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public List<User> getAllUsers() {
        log.debug("Admin requested all users list");

        List<User> users = userRepository.findAll();

        log.debug("Retrieved {} users", users.size());
        return users;
    }

    // Получение пользователя по ID (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public User getUserById(Long id) {
        log.debug("Admin requested user by id: {}", id);

        return userRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("User not found: id={}", id);
                    return new ResourceNotFoundException("User", "id", id);
                });
    }

    // Удаление пользователя по ID (только для администратора)
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUserById(Long id) {
        log.info("Admin attempting to delete user: id={}", id);

        if (!userRepository.existsById(id)) {
            log.warn("Cannot delete user - not found: id={}", id);
            throw new ResourceNotFoundException("User", "id", id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully: id={}", id);
    }

    // Обновление информации о пользователе
    public User update(User user) {
        log.info("Updating user: userId={}, username={}",
                user.getUserId(), user.getUsername());

        if (!userRepository.existsById(user.getUserId())) {
            log.warn("Cannot update user - not found: userId={}", user.getUserId());
            throw new ResourceNotFoundException("User", "id", user.getUserId());
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User updatedUser = userRepository.save(user);

        log.info("User updated successfully: userId={}", updatedUser.getUserId());
        return updatedUser;
    }

    // Установка основного счета пользователя
    @PreAuthorize("@cardSecurity.isOwner(#cardNumber)")
    public CardDto setMainCard(String cardNumber) {
        log.info("User attempting to set main card: cardNumber={}", cardNumber);

        User user = getCurrentUser();

        DebitCard card = (DebitCard) cardRepository
                .findByCardNumberAndCardType(cardNumber, CardType.DEBIT)
                .orElseThrow(() -> {
                    log.warn("Card not found when setting main card: cardNumber={}, userId={}",
                            cardNumber, user.getUserId());
                    return new ResourceNotFoundException("Card", "id", cardNumber);
                });

        user.setMainCard(card);
        userRepository.save(user);

        log.info("Main card set successfully: userId={}, cardNumber={}",
                user.getUserId(), cardNumber);

        return CardMapper.toDto(user.getMainCard());
    }

    // Получение текущего пользователя из контекста безопасности
    public User getCurrentUser() {
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
}