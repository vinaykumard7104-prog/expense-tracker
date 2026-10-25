package com.expensetracker.service;

import com.expensetracker.dto.Dtos.CategoryTotal;
import com.expensetracker.dto.Dtos.MonthPoint;
import com.expensetracker.dto.Dtos.Summary;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {
    private static final Long USER = 10L;

    @Mock TransactionRepository transactions;
    @InjectMocks ReportService service;

    @Test
    void summary_computesBalanceAndSavingsRate() {
        when(transactions.total(eq(USER), eq(TransactionType.INCOME), any(), any())).thenReturn(new BigDecimal("50000"));
        when(transactions.total(eq(USER), eq(TransactionType.EXPENSE), any(), any())).thenReturn(new BigDecimal("12500"));

        Summary s = service.summary(USER, YearMonth.now());

        assertEquals(new BigDecimal("37500"), s.balance());
        assertEquals(75, s.savingsRate());
    }

    @Test
    void summary_withNoIncome_doesNotDivideByZero() {
        when(transactions.total(eq(USER), eq(TransactionType.INCOME), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactions.total(eq(USER), eq(TransactionType.EXPENSE), any(), any())).thenReturn(new BigDecimal("300"));

        Summary s = service.summary(USER, YearMonth.now());

        assertEquals(0, s.savingsRate());
        assertEquals(new BigDecimal("-300"), s.balance());
    }

    @Test
    void byCategory_mapsRowsToDtos() {
        Object[] row = {"Food", new BigDecimal("4000")};
        when(transactions.expenseByCategory(eq(USER), any(), any())).thenReturn(Collections.singletonList(row));

        List<CategoryTotal> result = service.byCategory(USER, YearMonth.now());

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).category());
        assertEquals(new BigDecimal("4000"), result.get(0).total());
    }

    @Test
    void trend_zeroFillsMonthsWithoutTransactions() {
        YearMonth now = YearMonth.now();
        Object[] row = {now.getYear(), now.getMonthValue(), TransactionType.INCOME, new BigDecimal("1000")};
        when(transactions.monthly(eq(USER), any())).thenReturn(Collections.singletonList(row));

        List<MonthPoint> points = service.trend(USER, 3);

        assertEquals(3, points.size());
        assertEquals(now.minusMonths(2).toString(), points.get(0).month());
        assertEquals(0, BigDecimal.ZERO.compareTo(points.get(0).income()));
        assertEquals(now.toString(), points.get(2).month());
        assertEquals(new BigDecimal("1000"), points.get(2).income());
        assertEquals(0, BigDecimal.ZERO.compareTo(points.get(2).expense()));
    }

    @Test
    void trend_limitsMonthsToTwentyFour() {
        when(transactions.monthly(eq(USER), any())).thenReturn(Collections.emptyList());
        assertEquals(24, service.trend(USER, 500).size());
        assertEquals(1, service.trend(USER, 0).size());
    }
}
