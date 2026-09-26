package com.meditrack.meditrack.repository;

import com.meditrack.meditrack.model.PurchaseOrder;
import com.meditrack.meditrack.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findBySupplier(Supplier supplier);

    List<PurchaseOrder> findByStatus(PurchaseOrder.OrderStatus status);

    List<PurchaseOrder> findByOrderReferenceContainingIgnoreCase(String reference);

    long countBySupplierAndStatus(Supplier supplier, PurchaseOrder.OrderStatus status);
}
