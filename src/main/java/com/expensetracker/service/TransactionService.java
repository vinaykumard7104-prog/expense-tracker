package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.*;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;

@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final BudgetRepository budgets;
    private final UserRepository users;

    @Transactional(readOnly = true)
    public Page<TransactionResponse> list(Long userId, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 500),
                Sort.by(Sort.Direction.DESC, "txnDate", "id"));
        return transactions.findByUserId(userId, pageable).map(t -> toDto(t, null));
    }

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest r) {
        Category category = resolveCategory(userId, r);
        Transaction t = new Transaction();
        t.setUser(users.getReferenceById(userId));
        apply(t, r, category);
        transactions.save(t);
        return toDto(t, alertFor(userId, category, r));
    }

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest r) {
        // ownership check: only the owner can edit
        Transaction t = transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        Category category = resolveCategory(userId, r);
        apply(t, r, category);
        transactions.save(t);
        return toDto(t, alertFor(userId, category, r));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Transaction t = transactions.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Transaction not found"));
        transactions.delete(t);
    }

    /** The category must belong to the logged-in user and match the transaction type. */
    private Category resolveCategory(Long userId, TransactionRequest r) {
        Category category = categories.findByIdAndUserId(r.categoryId(), userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Category not found"));
        if (category.getType() != r.type()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Category type does not match transaction type");
        }
        return category;
    }

    private void apply(Transaction t, TransactionRequest r, Category category) {
        t.setAmount(r.amount());
        t.setType(r.type());
        t.setDescription(r.description());
        t.setTxnDate(r.txnDate());
        t.setCategory(category);
    }

    private String alertFor(Long userId, Category category, TransactionRequest r) {
        return r.type() == TransactionType.EXPENSE ? budgetAlert(userId, category, r.txnDate()) : null;
    }

    private String budgetAlert(Long userId, Category category, LocalDate date) {
        Budget b = budgets.findByUserIdAndCategoryId(userId, category.getId()).orElse(null);
        if (b == null) return null;
        YearMonth ym = YearMonth.from(date);
        BigDecimal spent = transactions.spent(userId, category.getId(), ym.atDay(1), ym.atEndOfMonth());
        int pct = spent.multiply(BigDecimal.valueOf(100))
                .divide(b.getMonthlyLimit(), 0, RoundingMode.DOWN).intValue();
        if (pct >= 100) return category.getName() + " budget exceeded (" + pct + "%)";
        if (pct >= 80) return "Heads up: " + category.getName() + " budget at " + pct + "%";
        return null;
    }

    private TransactionResponse toDto(Transaction t, String alert) {
        return new TransactionResponse(t.getId(), t.getAmount(), t.getType(), t.getDescription(), t.getTxnDate(),
                t.getCategory().getId(), t.getCategory().getName(), alert);
    }
}
