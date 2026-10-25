package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {
    private final TransactionRepository transactions;

    public Summary summary(Long userId, YearMonth month) {
        BigDecimal income = transactions.total(userId, TransactionType.INCOME, month.atDay(1), month.atEndOfMonth());
        BigDecimal expense = transactions.total(userId, TransactionType.EXPENSE, month.atDay(1), month.atEndOfMonth());
        BigDecimal balance = income.subtract(expense);
        int rate = income.signum() > 0
                ? balance.multiply(BigDecimal.valueOf(100)).divide(income, 0, RoundingMode.DOWN).intValue() : 0;
        return new Summary(income, expense, balance, rate);
    }

    public List<CategoryTotal> byCategory(Long userId, YearMonth month) {
        return transactions.expenseByCategory(userId, month.atDay(1), month.atEndOfMonth()).stream()
                .map(r -> new CategoryTotal((String) r[0], new BigDecimal(r[1].toString())))
                .toList();
    }

    /** Income and expense per month for the last N months (zero-filled), feeds the dashboard sparkline. */
    public List<MonthPoint> trend(Long userId, int months) {
        int n = Math.max(1, Math.min(months, 24));
        YearMonth start = YearMonth.now().minusMonths(n - 1L);
        Map<YearMonth, BigDecimal[]> buckets = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            buckets.put(start.plusMonths(i), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Object[] r : transactions.monthly(userId, start.atDay(1))) {
            YearMonth ym = YearMonth.of(((Number) r[0]).intValue(), ((Number) r[1]).intValue());
            BigDecimal[] v = buckets.get(ym);
            if (v == null) continue;
            BigDecimal amount = new BigDecimal(r[3].toString());
            if (r[2] == TransactionType.INCOME) v[0] = amount; else v[1] = amount;
        }
        return buckets.entrySet().stream()
                .map(e -> new MonthPoint(e.getKey().toString(), e.getValue()[0], e.getValue()[1]))
                .toList();
    }
}
