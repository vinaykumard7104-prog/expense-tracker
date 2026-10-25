package com.expensetracker.repository;

import com.expensetracker.entity.Budget;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    @EntityGraph(attributePaths = "category")
    List<Budget> findByUserId(Long userId);
    Optional<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
}
