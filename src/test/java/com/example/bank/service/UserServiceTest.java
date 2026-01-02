package com.example.bank.service;

import com.example.bank.Enums.Role;
import com.example.bank.exception.ResourceNotFoundException;
import com.example.bank.model.card.debitCard.DebitCard;
import com.example.bank.Enums.CardType;
import com.example.bank.model.user.CreateUserDto;
import com.example.bank.model.user.User;
import com.example.bank.model.user.UserDto;
import com.example.bank.repository.CardRepository;
import com.example.bank.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CardRepository cardRepository;
    
    private DebitCard card;



    @InjectMocks
    private UserService userService;

    private User expectedUser;
    private UserDto expectedUserDto;
    private CreateUserDto createUserDto;

    @BeforeEach
    void setUp() {



        // Подготовка DTO
        createUserDto = new CreateUserDto();
        createUserDto.setFirstName("John");
        createUserDto.setLastName("Doe");
        createUserDto.setEmail("john.doe@example.com");
        createUserDto.setUsername("johndoe");
        createUserDto.setPassword("password");
        createUserDto.setBlocked(null); // Проверим, что сервис установит false
        createUserDto.setRole(Role.USER);

        // Подготовка ожидаемого пользователя
        expectedUser = new User();
        expectedUser.setUserId(1L);
        expectedUser.setFirstName("John");
        expectedUser.setLastName("Doe");
        expectedUser.setEmail("john.doe@example.com");
        expectedUser.setUsername("johndoe");
        expectedUser.setPassword("encodedPassword");
        expectedUser.setBlocked(false);
        expectedUser.setRole(Role.USER);

        // Подготовка ожидаемого UserDto
        expectedUserDto = new UserDto();
        expectedUserDto.setUserId(1L);
        expectedUserDto.setFirstName("John");
        expectedUserDto.setLastName("Doe");
        expectedUserDto.setEmail("john.doe@example.com");
        expectedUserDto.setUsername("johndoe");
        expectedUserDto.setBlocked(false);
        expectedUserDto.setRole(Role.USER);


        card = new DebitCard();
        card.setId(1L);
        card.setCardNumber("1234567890");
        card.setUser(expectedUser);
        card.setExpiryDate(LocalDate.now().plusYears(1));
        card.setStatus(com.example.bank.Enums.CardStatus.ACTIVE);
    }

    @Test
    void testCreateUser() {
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        UserDto actualUserDto = userService.createUser(createUserDto);

        // Проверка, что ID пользователя корректно возвращается
        assertNotNull(actualUserDto);
        assertEquals(expectedUserDto.getUserId(), actualUserDto.getUserId());
        assertEquals(expectedUserDto.getFirstName(), actualUserDto.getFirstName());
        assertEquals(expectedUserDto.getLastName(), actualUserDto.getLastName());
        assertEquals(expectedUserDto.getEmail(), actualUserDto.getEmail());
        assertEquals(expectedUserDto.getUsername(), actualUserDto.getUsername());
        assertEquals(false, actualUserDto.getBlocked());
        assertEquals(Role.USER, actualUserDto.getRole());

        verify(passwordEncoder, times(1)).encode("password");
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testCreateUserWithBlockedSetToNull() {
        // Arrange
        createUserDto.setBlocked(null);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);

        // Act
        UserDto actualUserDto = userService.createUser(createUserDto);

        // Assert - проверяем, что status установлен в false
        assertFalse(actualUserDto.getBlocked());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testGetAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(expectedUser));

        List<User> users = userService.getAllUsers();

        assertThat(users).hasSize(1).contains(expectedUser);
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(expectedUser));

        User user = userService.getUserById(1L);

        assertEquals(expectedUser, user);
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void setMainCardTest() {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn("johndoe");
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(expectedUser);

        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findByUsername("johndoe")).thenReturn(Optional.of(expectedUser));
        when(cardRepository.findByCardNumberAndCardType("1234567890", CardType.DEBIT))
                .thenReturn(Optional.of(card));
        when(userRepository.save(any(User.class))).thenReturn(expectedUser);


        userService.setMainCard("1234567890");
        DebitCard card2 = expectedUser.getMainCard();
        // Проверка, что основная карта корректно устанавливается для пользователя
        assertEquals(card2, card);
    }

    @Test
    void deleteUserById_UserExists_DeletesUser() {
        when(userRepository.existsById(1L)).thenReturn(true);

        userService.deleteUserById(1L);

        // Проверка, что метод удаления был вызван
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUserById_UserNotExists_ThrowsException() {
        when(userRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUserById(99L);
        });

        // Проверка, что удаление не происходит при отсутствии пользователя
        verify(userRepository, never()).deleteById(anyLong());
    }





}