package com.expensetracker.controller;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService service;

    @GetMapping
    public PagedModel<TransactionResponse> list(@AuthenticationPrincipal Long userId,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return new PagedModel<>(service.list(userId, page, size));
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody TransactionRequest r) {
        return service.create(userId, r);
    }

    @PutMapping("/{id}")
    public TransactionResponse update(@AuthenticationPrincipal Long userId, @PathVariable Long id,
                                      @Valid @RequestBody TransactionRequest r) {
        return service.update(userId, id, r);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        service.delete(userId, id);
    }
}
