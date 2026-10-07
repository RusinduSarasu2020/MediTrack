package com.meditrack.repository;

import com.meditrack.model.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GoodsReceiptRepository extends JpaRepository<GoodsReceipt, Long> {
    boolean existsByReceiptKey(String receiptKey);
    Optional<GoodsReceipt> findByReceiptKey(String receiptKey);
    List<GoodsReceipt> findByPurchaseOrderId(Long purchaseOrderId);
    long countByPurchaseOrderId(Long purchaseOrderId);
}
