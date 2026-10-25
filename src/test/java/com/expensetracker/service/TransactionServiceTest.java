package com.expensetracker.service;

import com.expensetracker.dto.Dtos.TransactionRequest;
import com.expensetracker.dto.Dtos.TransactionResponse;
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
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    private static final Long USER = 10L;

    @Mock TransactionRepository transactions;
    @Mock CategoryRepository categories;
    @Mock BudgetRepository budgets;
    @Mock UserRepository users;
    @InjectMocks TransactionService service;

    private Category category(Long id, String name, TransactionType type) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setType(type);
        return c;
    }

    private Budget budget(String limit) {
        Budget b = new Budget();
        b.setMonthlyLimit(new BigDecimal(limit));
        return b;
    }

    private TransactionRequest expense(String amount) {
        return new TransactionRequest(new BigDecimal(amount), TransactionType.EXPENSE, "Lunch", LocalDate.now(), 1L);
    }

    private void givenFoodCategoryAndBudget(String limit, String alreadySpent) {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(1L, "Food", TransactionType.EXPENSE)));
        when(users.getReferenceById(USER)).thenReturn(new User());
        when(budgets.findByUserIdAndCategoryId(USER, 1L)).thenReturn(Optional.of(budget(limit)));
        when(transactions.spent(eq(USER), eq(1L), any(), any())).thenReturn(new BigDecimal(alreadySpent));
    }

    @Test
    void create_savesTransactionWithCorrectFieldsAndOwner() {
        givenFoodCategoryAndBudget("8000", "500");
        User owner = new User();
        when(users.getReferenceById(USER)).thenReturn(owner);

        service.create(USER, expense("500"));

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactions).save(captor.capture());
        Transaction saved = captor.getValue();
        assertEquals(new BigDecimal("500"), saved.getAmount());
        assertEquals(TransactionType.EXPENSE, saved.getType());
        assertEquals("Lunch", saved.getDescription());
        assertSame(owner, saved.getUser());
        assertEquals("Food", saved.getCategory().getName());
    }

    @Test
    void create_underEightyPercent_hasNoBudgetAlert() {
        givenFoodCategoryAndBudget("8000", "4000");
        TransactionResponse r = service.create(USER, expense("500"));
        assertNull(r.budgetAlert());
        assertEquals("Food", r.categoryName());
    }

    @Test
    void create_atEightyFivePercent_returnsWarning() {
        givenFoodCategoryAndBudget("8000", "6800");
        TransactionResponse r = service.create(USER, expense("100"));
        assertEquals("Heads up: Food budget at 85%", r.budgetAlert());
    }

    @Test
    void create_overBudget_returnsExceededAlert() {
        givenFoodCategoryAndBudget("8000", "9000");
        TransactionResponse r = service.create(USER, expense("100"));
        assertEquals("Food budget exceeded (112%)", r.budgetAlert());
    }

    @Test
    void create_incomeSkipsBudgetCheck() {
        when(categories.findByIdAndUserId(2L, USER)).thenReturn(Optional.of(category(2L, "Salary", TransactionType.INCOME)));
        when(users.getReferenceById(USER)).thenReturn(new User());
        var req = new TransactionRequest(new BigDecimal("50000"), TransactionType.INCOME, "Pay", LocalDate.now(), 2L);

        TransactionResponse r = service.create(USER, req);

        assertNull(r.budgetAlert());
        verifyNoInteractions(budgets);
    }

    @Test
    void create_categoryNotOwnedByUser_throwsNotFoundAndSavesNothing() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.create(USER, expense("100")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(transactions, never()).save(any());
    }

    @Test
    void create_categoryTypeMismatch_throwsBadRequest() {
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(1L, "Salary", TransactionType.INCOME)));

        ApiException ex = assertThrows(ApiException.class, () -> service.create(USER, expense("100")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(transactions, never()).save(any());
    }

    @Test
    void delete_ownTransaction_deletesIt() {
        Transaction t = new Transaction();
        when(transactions.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(t));
        service.delete(USER, 5L);
        verify(transactions).delete(t);
    }

    @Test
    void delete_someoneElsesTransaction_throwsNotFound() {
        when(transactions.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.delete(USER, 5L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(transactions, never()).delete(any(Transaction.class));
    }

    @Test
    void update_ownTransaction_changesFieldsAndSaves() {
        Transaction existing = new Transaction();
        existing.setAmount(new BigDecimal("100"));
        when(transactions.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(existing));
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(1L, "Food", TransactionType.EXPENSE)));
        when(budgets.findByUserIdAndCategoryId(USER, 1L)).thenReturn(Optional.of(budget("8000")));
        when(transactions.spent(eq(USER), eq(1L), any(), any())).thenReturn(new BigDecimal("250"));

        TransactionResponse r = service.update(USER, 5L, expense("250"));

        assertEquals(new BigDecimal("250"), existing.getAmount());
        assertEquals("Lunch", existing.getDescription());
        assertNull(r.budgetAlert());
        verify(transactions).save(existing);
    }

    @Test
    void update_someoneElsesTransaction_throwsNotFound() {
        when(transactions.findByIdAndUserId(5L, USER)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.update(USER, 5L, expense("250")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(transactions, never()).save(any());
    }

    @Test
    void update_typeMismatch_throwsBadRequest() {
        when(transactions.findByIdAndUserId(5L, USER)).thenReturn(Optional.of(new Transaction()));
        when(categories.findByIdAndUserId(1L, USER)).thenReturn(Optional.of(category(1L, "Salary", TransactionType.INCOME)));

        ApiException ex = assertThrows(ApiException.class, () -> service.update(USER, 5L, expense("250")));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(transactions, never()).save(any());
    }
}
