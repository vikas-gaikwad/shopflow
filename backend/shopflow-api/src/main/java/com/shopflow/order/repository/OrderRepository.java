package com.shopflow.order.repository;

import com.shopflow.order.entity.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<CustomerOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(
            @Param("id") Long id
    );

    @Query("""
        select o
        from CustomerOrder o
        left join fetch o.items
        where o.id = :id
        """)
    Optional<CustomerOrder> findByIdWithItems(
            @Param("id") Long id
    );

    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );
}