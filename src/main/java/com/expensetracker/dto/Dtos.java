package com.expensetracker.dto;

import com.expensetracker.entity.TransactionType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/** All request/response records in one place. Entities are never exposed to the client. */
public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(@NotBlank @Size(max = 100) String name,
                                  @NotBlank @Email String email,
                                  @NotBlank @Size(min = 6, max = 72) String password) {}
    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record AuthResponse(String token, String name, String email) {}

    public record CategoryResponse(Long id, String name, TransactionType type) {}

    public record TransactionRequest(@NotNull @DecimalMin("0.01") BigDecimal amount,
                                     @NotNull TransactionType type,
                                     @Size(max = 255) String description,
                                     @NotNull LocalDate txnDate,
                                     @NotNull Long categoryId) {}
    public record TransactionResponse(Long id, BigDecimal amount, TransactionType type, String description,
                                      LocalDate txnDate, Long categoryId, String categoryName, String budgetAlert) {}

    public record BudgetRequest(@NotNull Long categoryId, @NotNull @DecimalMin("1") BigDecimal monthlyLimit) {}
    public record BudgetResponse(Long categoryId, String categoryName, BigDecimal monthlyLimit,
                                 BigDecimal spent, int percent) {}

    public record Summary(BigDecimal income, BigDecimal expense, BigDecimal balance, int savingsRate) {}
    public record CategoryTotal(String category, BigDecimal total) {}
    public record MonthPoint(String month, BigDecimal income, BigDecimal expense) {}

    public record ApiError(int status, String message) {}
}
