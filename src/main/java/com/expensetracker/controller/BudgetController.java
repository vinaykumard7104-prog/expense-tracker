package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
@RequiredArgsConstructor
public class BudgetController {
    private final BudgetService service;

    @GetMapping
    public List<BudgetResponse> list(@AuthenticationPrincipal Long userId) { return service.list(userId); }

    @PutMapping
    public BudgetResponse upsert(@AuthenticationPrincipal Long userId, @Valid @RequestBody BudgetRequest r) {
        return service.upsert(userId, r);
    }
}
