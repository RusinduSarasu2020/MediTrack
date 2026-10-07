package com.meditrack.dto;

import java.math.BigDecimal;

public class CartItemDto {
    private Long medicineId;
    private String sku;
    private String name;
    private BigDecimal unitPrice;
    private int quantity;
    private boolean prescriptionRequired;

    public CartItemDto() {}

    public CartItemDto(Long medicineId, String sku, String name, BigDecimal unitPrice, int quantity, boolean prescriptionRequired) {
        this.medicineId = medicineId;
        this.sku = sku;
        this.name = name;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.prescriptionRequired = prescriptionRequired;
    }

    public Long getMedicineId() { return medicineId; }
    public void setMedicineId(Long medicineId) { this.medicineId = medicineId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public boolean isPrescriptionRequired() { return prescriptionRequired; }
    public void setPrescriptionRequired(boolean prescriptionRequired) { this.prescriptionRequired = prescriptionRequired; }

    public BigDecimal getSubtotal() {
        if (unitPrice == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
