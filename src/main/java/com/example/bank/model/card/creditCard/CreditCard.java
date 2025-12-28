package com.example.bank.model.card.creditCard;

import com.example.bank.model.card.Card;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// Сущность кредитного счета
// Представляет кредитную карту или кредитный счет с возможностью задолженности


/**
 ВАЖНО!!!!!!!!!!!!!!!!!!!!!!!!! БАЛАНС У НАС ЭТО ВСЯ СУММА КОТОРУЮ МОЖЕТ ПОТРАТИТЬ ПОЛЬЗОВАТЕЛЬ ТОЕСТЬ КРЕДИТ ЛИМИТ ИЛИ ЕГО ОСТАТОК + ЕСЛИ ЕСТЬ ПЛЮСОВЫЕ ДЕНЬГИ
 **/
@Data
@NoArgsConstructor
@Entity
@Table(name = "credit_cards")
public class CreditCard extends Card {

    // Кредитный лимит счета
    @NotNull
    private BigDecimal creditLimit;
    
    // Процентная ставка по кредиту (годовая)
    @NotNull
    private BigDecimal interestRate;
    
    // Минимальный процентный платеж (по умолчанию 5%)
    @NotNull
    private BigDecimal minimumPaymentRate = BigDecimal.valueOf(5);
    
    // Грейс-период в днях пока нет реализации скрываем
    @JsonIgnore
    @NotNull
    private Integer gracePeriod = 0;
    

    // Общая сумма задолженности (основной долг + проценты)
    @NotNull
    private BigDecimal totalDebt = BigDecimal.ZERO;
    
    // Начисленные проценты
    @NotNull
    private BigDecimal accruedInterest = BigDecimal.ZERO;

    // Текущая задолженность по кредиту
    @NotNull
    private BigDecimal debt = BigDecimal.ZERO;
    
    // Дата следующего платежа (первое число следующего месяца)
    @NotNull
    private LocalDate paymentDueDate = LocalDate.now().plusMonths(1).withDayOfMonth(1);


    // ==================== Бизнес-методы ====================

    // Обновляет значение задолженности на основе текущего баланса
    // Если баланс положительный - это переплата или доступные средства, долг = 0
    // Если баланс отрицательный - это задолженность, равная абсолютному значению баланса
    public void updateTotalDebt() {
        this.totalDebt = debt.add(accruedInterest);
    }

    // Начисляет проценты на текущую задолженность
    // Проценты начисляются только при наличии задолженности
    public void accrueInterest() {
        if (debt.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Не начисляем проценты при отсутствии долга или переплате
        }

        // месячная ставка = годовая / 12
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        // проценты за месяц
        BigDecimal interest = debt.multiply(monthlyRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Только добавляем начисленные проценты
        this.accruedInterest = this.accruedInterest.add(interest);

    }

    // ==================== Вспомогательные методы ====================

    // Проверяет, есть ли переплата на счете
    // true, если на счете есть положительный баланс (переплата)
    public boolean hasOverpayment() {
        return getBalance().compareTo(BigDecimal.ZERO) > 0;
    }

    // Проверяет, есть ли задолженность по счету
    // true, если баланс счета отрицательный (долг)
    public boolean hasDebt() {
        return getTotalDebt().compareTo(BigDecimal.ZERO) > 0;
    }



    // Проверяет, превышен ли кредитный лимит
    // true, если баланс превышает кредитный лимит
    public boolean isOverCreditLimit() {
        return getBalance().compareTo(creditLimit) > 0;
    }
}