package com.example.bank.integration;

import com.example.bank.model.user.CreateUserDto;
import com.example.bank.model.user.User;
import com.example.bank.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DebitCardIntegrationTest {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private String adminToken;
    private String userToken;
    private String debitCardNumber;

    @BeforeEach
    public void setUp() throws Exception {
        // Получаем токен администратора
        adminToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.example.bank.model.user.LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Создаем обычного пользователя
        CreateUserDto userDto = new CreateUserDto();
        userDto.setUsername("testuser2");
        userDto.setPassword("password123");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("testuser@example.com");
        userDto.setPhoneNumber("1234567890");

        String userResponse = mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Получаем токен обычного пользователя
        userToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.example.bank.model.user.LoginRequest("testuser2", "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        User user = userRepository.findByUsername("testuser2").orElseThrow();
        userId = user.getUserId();


        // Создаем дебетовый аккаунт
        String cardResponse = mockMvc.perform(post("/api/debit-cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Извлекаем номер счета из ответа
        JsonNode cardJson = objectMapper.readTree(cardResponse);
        debitCardNumber = cardJson.get("cardNumber").asText();
    }

    @Test
    public void CreateDebitCard_asUser_createsCard() throws Exception {
        mockMvc.perform(post("/api/debit-cards")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").exists())
                .andExpect(jsonPath("$.balance").value(0.00));
    }


    @Test
    public void deleteDebitCard_asUser_deletesCard() throws Exception {
        mockMvc.perform(delete("/api/debit-cards/{cardNumber}", debitCardNumber)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }


}