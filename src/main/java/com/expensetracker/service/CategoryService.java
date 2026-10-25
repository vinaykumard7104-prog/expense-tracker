package com.expensetracker.service;

import com.expensetracker.dto.Dtos.CategoryResponse;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categories;

    @Transactional(readOnly = true)
    public List<CategoryResponse> list(Long userId) {
        return categories.findByUserIdOrderByNameAsc(userId).stream()
                .map(c -> new CategoryResponse(c.getId(), c.getName(), c.getType()))
                .toList();
    }
}
