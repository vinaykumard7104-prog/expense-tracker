package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // EntityGraph loads the category in the same query (avoids the N+1 problem)
    @EntityGraph(attributePaths = "category")
    Page<Transaction> findByUserId(Long userId, Pageable pageable);

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    @Query("select coalesce(sum(t.amount),0) from Transaction t where t.user.id = :uid and t.type = :type "
         + "and t.txnDate between :from and :to")
    BigDecimal total(@Param("uid") Long uid, @Param("type") TransactionType type,
                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select t.category.name, sum(t.amount) from Transaction t where t.user.id = :uid "
         + "and t.type = com.expensetracker.entity.TransactionType.EXPENSE and t.txnDate between :from and :to "
         + "group by t.category.name order by sum(t.amount) desc")
    List<Object[]> expenseByCategory(@Param("uid") Long uid, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select coalesce(sum(t.amount),0) from Transaction t where t.user.id = :uid and t.category.id = :cid "
         + "and t.type = com.expensetracker.entity.TransactionType.EXPENSE and t.txnDate between :from and :to")
    BigDecimal spent(@Param("uid") Long uid, @Param("cid") Long cid,
                     @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select year(t.txnDate), month(t.txnDate), t.type, sum(t.amount) from Transaction t "
         + "where t.user.id = :uid and t.txnDate >= :from "
         + "group by year(t.txnDate), month(t.txnDate), t.type")
    List<Object[]> monthly(@Param("uid") Long uid, @Param("from") LocalDate from);
}
