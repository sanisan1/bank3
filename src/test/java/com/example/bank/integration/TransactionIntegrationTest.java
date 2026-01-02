package com.example.bank.integration;

import com.example.bank.model.card.creditCard.CreditCardCreateRequest;
import com.example.bank.model.transaction.TransactionOperationRequest;
import com.example.bank.model.transaction.TransferRequest;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "bank.credit.default.interest-rate=15.0",
        "bank.credit.default.limit=10000.0",
        "bank.credit.default.minimum-payment-rate=5.0",
        "bank.credit.default.grace-period=30"
})
public class TransactionIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private String adminToken;
    private String userToken;
    private String creditCardNumber;
    private String debitCardNumber;
    private Long creditCardId;
    private Long debitCardId;

    @BeforeEach
    public void setUp() throws Exception {
        adminToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.example.bank.model.user.LoginRequest("admin", "admin123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .trim();

        // Создаем обычного пользователя
        CreateUserDto userDto = new CreateUserDto();
        userDto.setUsername("testuser2");
        userDto.setPassword("password123");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("testuser@example.com");
        userDto.setPhoneNumber("1234567890");

        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk());

        userToken = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.example.bank.model.user.LoginRequest("testuser2", "password123"))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .trim();

        User user = userRepository.findByUsername("testuser2").orElseThrow();
        userId = user.getUserId();

        // Создаем кредитный аккаунт через администратора
        CreditCardCreateRequest request = new CreditCardCreateRequest();
        request.setUserId(userId);
        request.setCreditLimit(new BigDecimal("5000.00"));
        request.setInterestRate(new BigDecimal("0.15"));
        request.setGracePeriod(30);

        String creditCardResponse = mockMvc.perform(post("/api/credit-cards/createforadmin")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode creditCardJson = objectMapper.readTree(creditCardResponse);
        creditCardNumber = creditCardJson.get("cardNumber").asText();
        JsonNode creditIdNode = creditCardJson.get("id");

        if (creditIdNode != null && !creditIdNode.isNull()) {
            creditCardId = creditIdNode.asLong();
        } else {
            String getAllCardsResponse = mockMvc.perform(get("/api/cards/getCards")
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            JsonNode getAllCardsJson = objectMapper.readTree(getAllCardsResponse);
            for (JsonNode cardNode : getAllCardsJson) {
                if (creditCardNumber.equals(cardNode.get("cardNumber").asText())) {
                    creditCardId = cardNode.get("id").asLong();
                    break;
                }
            }
        }

        String debitCardResponse = mockMvc.perform(post("/api/debit-cards/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode debitCardJson = objectMapper.readTree(debitCardResponse);
        debitCardNumber = debitCardJson.get("cardNumber").asText();
        JsonNode debitIdNode = debitCardJson.get("id");

        if (debitIdNode != null && !debitIdNode.isNull()) {
            debitCardId = debitIdNode.asLong();
        } else {
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
    public void depositToCard_asUser_succeeds() throws Exception {
        TransactionOperationRequest request = new TransactionOperationRequest();
        request.setId(debitCardId);
        request.setAmount(new BigDecimal("100.00"));
        request.setComment("Test deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(debitCardNumber))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    public void withdrawFromCard_asUser_succeeds() throws Exception {
        // Сначала пополняем счет
        TransactionOperationRequest depositRequest = new TransactionOperationRequest();
        depositRequest.setId(debitCardId);
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setComment("Initial deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Затем снимаем средства
        TransactionOperationRequest withdrawRequest = new TransactionOperationRequest();
        withdrawRequest.setId(debitCardId);
        withdrawRequest.setAmount(new BigDecimal("50.00"));
        withdrawRequest.setComment("Test withdrawal");

        mockMvc.perform(post("/api/transactions/withdraw")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(debitCardNumber))
                .andExpect(jsonPath("$.balance").value(50.00));
    }

    @Test
    public void transferBetweenCards_asUser_succeeds() throws Exception { // Тест перевода между картами
        // Сначала пополняем первый счет
        TransactionOperationRequest depositRequest = new TransactionOperationRequest();
        depositRequest.setId(debitCardId);
        depositRequest.setAmount(new BigDecimal("200.00"));
        depositRequest.setComment("Initial deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Переводим средства со счета на счет
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setFromId(debitCardId);
        transferRequest.setToId(creditCardId);
        transferRequest.setAmount(new BigDecimal("100.00"));
        transferRequest.setComment("Test transfer");

        mockMvc.perform(post("/api/transactions/transfer")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardNumber").value(debitCardNumber))
                .andExpect(jsonPath("$.balance").value(100.00));
    }

    @Test
    public void getTransactionsByCard_asUser_succeeds() throws Exception { // Тест получения транзакций для пользователя
        // Сначала выполняем операцию, чтобы были транзакции
        TransactionOperationRequest depositRequest = new TransactionOperationRequest();
        depositRequest.setId(debitCardId);
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setComment("Test deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Получаем список транзакций для пользователя (так как нельзя получить по маскированному номеру карты)
        mockMvc.perform(get("/api/transactions/getAllForUser")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].amount").value(100.00));
    }

    @Test
    public void getAllTransactions_asAdmin_succeeds() throws Exception { // Тест получения всех транзакций администратором
        // Сначала выполняем операцию, чтобы были транзакции
        TransactionOperationRequest depositRequest = new TransactionOperationRequest();
        depositRequest.setId(debitCardId);
        depositRequest.setAmount(new BigDecimal("100.00"));
        depositRequest.setComment("Test deposit");

        mockMvc.perform(post("/api/transactions/deposit")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositRequest)))
                .andExpect(status().isOk());

        // Админ получает все транзакции
        mockMvc.perform(get("/api/transactions/getAll")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }


}