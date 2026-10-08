package com.meditrack.repository;

import com.meditrack.enums.OrderStatus;
import com.meditrack.model.CustomerOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<CustomerOrder> findByStatusOrderByCreatedAtDesc(OrderStatus status);
    Optional<CustomerOrder> findByIdAndCustomerId(Long id, Long customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CustomerOrder o WHERE o.id = :id")
    Optional<CustomerOrder> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(OrderStatus status);

    List<CustomerOrder> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(o) FROM CustomerOrder o WHERE o.createdAt >= :startDate AND o.createdAt < :endDate")
    int countOrdersBetween(@Param("startDate") Instant startDate, @Param("endDate") Instant endDate);
}
