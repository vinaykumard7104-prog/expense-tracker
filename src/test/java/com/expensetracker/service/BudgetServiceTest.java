package com.expensetracker.service;

import com.expensetracker.dto.Dtos.BudgetRequest;
import com.expensetracker.dto.Dtos.BudgetResponse;
import com.expensetracker.entity.*;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {
    private static final Long USER = 10L;

    @Mock BudgetRepository budgets;
    @Mock CategoryRepository categories;
    @Mock TransactionRepository transactions;
    @Mock UserRepository users;
    @InjectMocks BudgetService service;

    private Category category(TransactionType type) {
        Category c = new Category();
        c.setId(1L);
        c.setName("Food");
        c.setType(type);
        return c;
    }

    private Budget budget(Category c, String limit) {
        Budget b = new Budget();
        b.setCategory(c);
        b.setMonthlyLimit(new BigDecimal(limit));
        return b;
    }

    @Test
    void list_computesSpentAndPercent() {
        when(budgets.findByUserId(USER)).thenReturn(List.of(budget(category(TransactionType.EXPENSE), "8000")));
        when(transactions.spent(eq(USER), eq(1L), any(), any())).thenReturn(new BigDecimal("6800"));

        BudgetResponse r = service.list(USER).get(0);

        assertEquals("Food", r.categoryName());
        assertEquals(85, r.percent());
        assertEquals(new BigDecimal("6800"), r.spent());
    }

    @Test
    void upsert_existingBudget_updatesLimit() {
        Category food = category(TransactionType.EXPENSE);
        Budget existing = budget(food, "8000");
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(food));
        when(budgets.findByUserIdAndCategoryId(USER, 1L)).thenReturn(Optional.of(existing));
        when(budgets.save(any(Budget.class))).thenAnswer(returnsFirstArg());
        when(transactions.spent(eq(USER), eq(1L), any(), any())).thenReturn(new BigDecimal("2000"));

        BudgetResponse r = service.upsert(USER, new BudgetRequest(1L, new BigDecimal("10000")));

        assertEquals(0, new BigDecimal("10000").compareTo(r.monthlyLimit()));
        assertEquals(20, r.percent());
    }

    @Test
    void upsert_noBudgetYet_createsOneForTheUser() {
        User owner = new User();
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(TransactionType.EXPENSE)));
        when(budgets.findByUserIdAndCategoryId(USER, 1L)).thenReturn(Optional.empty());
        when(users.getReferenceById(USER)).thenReturn(owner);
        when(budgets.save(any(Budget.class))).thenAnswer(returnsFirstArg());
        when(transactions.spent(eq(USER), eq(1L), any(), any())).thenReturn(BigDecimal.ZERO);

        service.upsert(USER, new BudgetRequest(1L, new BigDecimal("5000")));

        ArgumentCaptor<Budget> captor = ArgumentCaptor.forClass(Budget.class);
        verify(budgets).save(captor.capture());
        assertSame(owner, captor.getValue().getUser());
        assertEquals(new BigDecimal("5000"), captor.getValue().getMonthlyLimit());
    }

    @Test
    void upsert_incomeCategory_throwsBadRequest() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(TransactionType.INCOME)));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsert(USER, new BudgetRequest(1L, new BigDecimal("5000"))));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(budgets, never()).save(any());
    }

    @Test
    void upsert_unknownCategory_throwsNotFound() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.upsert(USER, new BudgetRequest(1L, new BigDecimal("5000"))));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
