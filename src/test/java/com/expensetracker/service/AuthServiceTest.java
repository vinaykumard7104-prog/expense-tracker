package com.expensetracker.service;

import com.expensetracker.dto.Dtos.AuthResponse;
import com.expensetracker.dto.Dtos.LoginRequest;
import com.expensetracker.dto.Dtos.RegisterRequest;
import com.expensetracker.entity.Budget;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.BudgetRepository;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserRepository;
import com.expensetracker.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock UserRepository users;
    @Mock CategoryRepository categories;
    @Mock BudgetRepository budgets;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;
    @InjectMocks AuthService auth;

    private User existingUser() {
        User u = new User();
        u.setId(5L);
        u.setName("Asha");
        u.setEmail("a@b.com");
        u.setPassword("HASH");
        return u;
    }

    @Test
    void register_newUser_hashesPasswordNormalisesEmailAndSeedsDefaults() {
        when(users.existsByEmail("a@b.com")).thenReturn(false);
        when(encoder.encode("secret1")).thenReturn("HASH");
        when(users.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(categories.save(any(Category.class))).thenAnswer(returnsFirstArg());
        when(jwt.generate(1L, "a@b.com")).thenReturn("TOKEN");

        AuthResponse res = auth.register(new RegisterRequest("  Asha ", "  A@B.COM ", "secret1"));

        assertEquals("TOKEN", res.token());
        assertEquals("Asha", res.name());
        assertEquals("a@b.com", res.email());

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertEquals("HASH", saved.getValue().getPassword());   // never stores the raw password
        verify(categories, times(7)).save(any(Category.class)); // 5 expense + 2 income categories
        verify(budgets, times(5)).save(any(Budget.class));      // one budget per expense category
    }

    @Test
    void register_duplicateEmail_throwsConflictAndSavesNothing() {
        when(users.existsByEmail("a@b.com")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> auth.register(new RegisterRequest("Asha", "a@b.com", "secret1")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(users, never()).save(any());
    }

    @Test
    void login_correctPassword_returnsToken() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches("secret1", "HASH")).thenReturn(true);
        when(jwt.generate(5L, "a@b.com")).thenReturn("TOKEN");

        AuthResponse res = auth.login(new LoginRequest("A@B.com", "secret1"));

        assertEquals("TOKEN", res.token());
        assertEquals("Asha", res.name());
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches("bad", "HASH")).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> auth.login(new LoginRequest("a@b.com", "bad")));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(jwt, never()).generate(any(), any());
    }

    @Test
    void login_unknownEmailAndWrongPasswordGiveTheSameMessage() {
        when(users.findByEmail("nobody@b.com")).thenReturn(Optional.empty());
        when(users.findByEmail("a@b.com")).thenReturn(Optional.of(existingUser()));
        when(encoder.matches(eq("bad"), eq("HASH"))).thenReturn(false);

        ApiException unknown = assertThrows(ApiException.class, () -> auth.login(new LoginRequest("nobody@b.com", "x")));
        ApiException wrong = assertThrows(ApiException.class, () -> auth.login(new LoginRequest("a@b.com", "bad")));

        // identical messages stop attackers from discovering which emails are registered
        assertEquals(unknown.getMessage(), wrong.getMessage());
    }
}
