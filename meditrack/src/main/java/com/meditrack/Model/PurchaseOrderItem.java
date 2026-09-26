package com.meditrack.meditrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single line item (medicine + quantity) within a Purchase Order.
 */
@Entity
@Table(name = "purchase_order_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @NotBlank(message = "Medicine name is required")
    private String medicineName;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @Min(value = 0, message = "Unit price cannot be negative")
    private double unitPrice;

    /** Current stock level at the time the order was raised (for reference) */
    private int currentStockLevel;

    public double getLineTotal() {
        return quantity * unitPrice;
    }
}
