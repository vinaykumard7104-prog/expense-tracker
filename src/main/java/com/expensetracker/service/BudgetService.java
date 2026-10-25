package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.*;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(Long userId) {
        return budgets.findByUserId(userId).stream().map(b -> toDto(userId, b)).toList();
    }

    @Transactional
    public BudgetResponse upsert(Long userId, BudgetRequest r) {
        Category c = categories.findByIdAndUserId(r.categoryId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (c.getType() != TransactionType.EXPENSE) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Budgets only apply to expense categories");
        }
        Budget b = budgets.findByUserIdAndCategoryId(userId, c.getId()).orElseGet(() -> {
            Budget n = new Budget();
            n.setUser(users.getReferenceById(userId));
            n.setCategory(c);
            return n;
        });
        b.setMonthlyLimit(r.monthlyLimit());
        return toDto(userId, budgets.save(b));
    }

    private BudgetResponse toDto(Long userId, Budget b) {
        YearMonth now = YearMonth.now();
        BigDecimal spent = transactions.spent(userId, b.getCategory().getId(), now.atDay(1), now.atEndOfMonth());
        int pct = spent.multiply(BigDecimal.valueOf(100)).divide(b.getMonthlyLimit(), 0, RoundingMode.DOWN).intValue();
        return new BudgetResponse(b.getCategory().getId(), b.getCategory().getName(), b.getMonthlyLimit(), spent, pct);
    }
}
