package com.meditrack.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicine_id", nullable = false)
    private Medicine medicine;

    @Column(nullable = false)
    private int orderedQty;

    @Column(nullable = false)
    private int receivedQty = 0;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    public PurchaseOrderItem() {}

    public PurchaseOrderItem(PurchaseOrder purchaseOrder, Medicine medicine, int orderedQty, BigDecimal unitCost) {
        this.purchaseOrder = purchaseOrder;
        this.medicine = medicine;
        this.orderedQty = orderedQty;
        this.unitCost = unitCost;
        this.receivedQty = 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public Medicine getMedicine() { return medicine; }
    public void setMedicine(Medicine medicine) { this.medicine = medicine; }

    public int getOrderedQty() { return orderedQty; }
    public void setOrderedQty(int orderedQty) { this.orderedQty = orderedQty; }

    public int getQuantityOrdered() { return orderedQty; }
    public void setQuantityOrdered(int quantityOrdered) { this.orderedQty = quantityOrdered; }

    public int getReceivedQty() { return receivedQty; }
    public void setReceivedQty(int receivedQty) { this.receivedQty = receivedQty; }

    public int getQuantityReceived() { return receivedQty; }
    public void setQuantityReceived(int quantityReceived) { this.receivedQty = quantityReceived; }

    public BigDecimal getUnitCost() { return unitCost; }
    public void setUnitCost(BigDecimal unitCost) { this.unitCost = unitCost; }

    public BigDecimal getLineTotal() {
        return unitCost != null ? unitCost.multiply(BigDecimal.valueOf(orderedQty)) : BigDecimal.ZERO;
    }
}
