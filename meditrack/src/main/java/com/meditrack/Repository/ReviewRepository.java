package com.meditrack.repository;

import com.meditrack.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByMedicineIdAndVisibleTrueOrderByUpdatedAtDesc(Long medicineId);
    List<Review> findByCustomerIdOrderByUpdatedAtDesc(Long customerId);
    Optional<Review> findByCustomerIdAndOrderIdAndMedicineId(Long customerId, Long orderId, Long medicineId);
    boolean existsByCustomerIdAndOrderIdAndMedicineId(Long customerId, Long orderId, Long medicineId);
}
