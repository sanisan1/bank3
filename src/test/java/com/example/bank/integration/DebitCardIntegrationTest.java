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
    private Long debitCardId;

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


        // Создаем дебетовый аккаунт через администратора
        String cardResponse = mockMvc.perform(post("/api/debit-cards/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Извлекаем номер счета и ID из ответа
        JsonNode cardJson = objectMapper.readTree(cardResponse);
        debitCardNumber = cardJson.get("cardNumber").asText();
        // Проверяем, есть ли ID в ответе, и если нет, то получаем через отдельный запрос
        JsonNode idNode = cardJson.get("id");
        if (idNode != null) {
            debitCardId = idNode.asLong();
        } else {
            // Если ID не возвращается в ответе на создание, получаем через отдельный запрос
            String getAllCardsResponse = mockMvc.perform(get("/api/cards/getCards")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();
            
            JsonNode getAllCardsJson = objectMapper.readTree(getAllCardsResponse);
            for (JsonNode cardNode : getAllCardsJson) {
                if (debitCardNumber.equals(cardNode.get("cardNumber").asText())) {
                    debitCardId = cardNode.get("id").asLong();
                    break;
                }
            }
        }
    }

    @Test
    public void CreateDebitCard_asAdmin_createsCard() throws Exception {
        mockMvc.perform(post("/api/debit-cards/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cardNumber").exists())
                .andExpect(jsonPath("$.balance").value(0.00));
    }


    @Test
    public void deleteDebitCard_asAdmin_deletesCard() throws Exception {
        mockMvc.perform(delete("/api/debit-cards/{id}", debitCardId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }


}