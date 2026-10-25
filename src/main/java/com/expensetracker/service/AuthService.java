package com.expensetracker.service;

import com.expensetracker.dto.Dtos.*;
import com.expensetracker.entity.*;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.*;
import com.expensetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final BudgetRepository budgets;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPassword(encoder.encode(r.password()));
        users.save(u);
        seedDefaults(u);
        return new AuthResponse(jwt.generate(u.getId(), email), u.getName(), email);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        String email = r.email().trim().toLowerCase();
        User u = users.findByEmail(email)
                .filter(x -> encoder.matches(r.password(), x.getPassword()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return new AuthResponse(jwt.generate(u.getId(), email), u.getName(), email);
    }

    /** Every new user gets starter categories and budgets so the dashboard works immediately. */
    private void seedDefaults(User u) {
        Map<String, Integer> expense = new LinkedHashMap<>();
        expense.put("Food", 8000); expense.put("Rent", 20000); expense.put("Travel", 5000);
        expense.put("Shopping", 6000); expense.put("Bills", 4000);
        expense.forEach((name, limit) -> {
            Category c = newCategory(u, name, TransactionType.EXPENSE);
            Budget b = new Budget();
            b.setUser(u);
            b.setCategory(c);
            b.setMonthlyLimit(BigDecimal.valueOf(limit));
            budgets.save(b);
        });
        List.of("Salary", "Freelance").forEach(n -> newCategory(u, n, TransactionType.INCOME));
    }

    private Category newCategory(User u, String name, TransactionType type) {
        Category c = new Category();
        c.setUser(u);
        c.setName(name);
        c.setType(type);
        return categories.save(c);
    }
}
