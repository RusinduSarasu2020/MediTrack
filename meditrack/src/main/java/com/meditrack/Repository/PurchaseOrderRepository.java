package com.meditrack.repository;

import com.meditrack.enums.PurchaseOrderStatus;
import com.meditrack.model.PurchaseOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    List<PurchaseOrder> findBySupplierIdOrderByCreatedAtDesc(Long supplierId);
    long countBySupplierId(Long supplierId);
    List<PurchaseOrder> findByStatus(PurchaseOrderStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT po FROM PurchaseOrder po WHERE po.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(poi.orderedQty - poi.receivedQty), 0) FROM PurchaseOrderItem poi WHERE poi.medicine.id = :medicineId AND poi.purchaseOrder.status IN ('ORDERED', 'PARTIALLY_RECEIVED')")
    int getOutstandingOrderedQty(@Param("medicineId") Long medicineId);
}
