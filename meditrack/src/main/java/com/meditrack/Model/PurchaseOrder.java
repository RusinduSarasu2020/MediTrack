package com.meditrack.meditrack.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a purchase order raised to replenish stock from a supplier.
 * Supports the workflow: DRAFT -> PENDING_APPROVAL -> APPROVED -> SENT -> IN_TRANSIT -> DELIVERED
 * (or CANCELLED at any point before DELIVERED).
 */
@Entity
@Table(name = "purchase_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    /** Human friendly reference, e.g. PO-2026-0001 */
    @Column(unique = true)
    private String orderReference;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.DRAFT;

    private LocalDate expectedDeliveryDate;

    private LocalDate actualDeliveryDate;

    @Column(length = 1000)
    private String deliveryNotes;

    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt = LocalDateTime.now();

    /** Name of staff member who approved this order (e.g. Supplier Coordinator / Admin) */
    private String approvedBy;

    public enum OrderStatus {
        DRAFT,
        PENDING_APPROVAL,
        APPROVED,
        SENT_TO_SUPPLIER,
        IN_TRANSIT,
        DELIVERED,
        CANCELLED
    }

    public double getTotalAmount() {
        return items.stream()
                .mapToDouble(i -> i.getQuantity() * i.getUnitPrice())
                .sum();
    }
}
