package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService service;

    @GetMapping("/summary")
    public Summary summary(@AuthenticationPrincipal Long userId,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.summary(userId, month != null ? month : YearMonth.now());
    }

    @GetMapping("/by-category")
    public List<CategoryTotal> byCategory(@AuthenticationPrincipal Long userId,
                                          @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth month) {
        return service.byCategory(userId, month != null ? month : YearMonth.now());
    }

    @GetMapping("/trend")
    public List<MonthPoint> trend(@AuthenticationPrincipal Long userId,
                                  @RequestParam(defaultValue = "6") int months) {
        return service.trend(userId, months);
    }
}
