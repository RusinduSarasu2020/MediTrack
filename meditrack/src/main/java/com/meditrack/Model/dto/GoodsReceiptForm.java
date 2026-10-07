package com.meditrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class GoodsReceiptForm {

    @NotNull(message = "Purchase order is required.")
    private Long purchaseOrderId;

    @NotBlank(message = "Receipt key is required.")
    private String receiptKey;

    private List<ItemRow> items = new ArrayList<>();

    public static class ItemRow {
        private Long purchaseOrderItemId;
        private String batchNumber;
        private LocalDate expiryDate;
        private BigDecimal costPrice;
        private BigDecimal sellingPrice;
        private Integer quantity;

        public Long getPurchaseOrderItemId() { return purchaseOrderItemId; }
        public void setPurchaseOrderItemId(Long purchaseOrderItemId) { this.purchaseOrderItemId = purchaseOrderItemId; }
        public String getBatchNumber() { return batchNumber; }
        public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
        public LocalDate getExpiryDate() { return expiryDate; }
        public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getSellingPrice() { return sellingPrice; }
        public void setSellingPrice(BigDecimal sellingPrice) { this.sellingPrice = sellingPrice; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }

    public Long getPurchaseOrderId() { return purchaseOrderId; }
    public void setPurchaseOrderId(Long purchaseOrderId) { this.purchaseOrderId = purchaseOrderId; }
    public String getReceiptKey() { return receiptKey; }
    public void setReceiptKey(String receiptKey) { this.receiptKey = receiptKey; }
    public List<ItemRow> getItems() { return items; }
    public void setItems(List<ItemRow> items) { this.items = items; }
}
