package com.meditrack.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "goods_receipts", uniqueConstraints = {
    @UniqueConstraint(columnNames = "receiptKey")
})
public class GoodsReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(nullable = false, length = 100)
    private String receiptKey;

    private Long invoiceFileId;

    @Column(nullable = false)
    private Instant receivedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "received_by", nullable = false)
    private User receivedBy;

    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GoodsReceiptItem> items = new ArrayList<>();

    public GoodsReceipt() {}

    public GoodsReceipt(PurchaseOrder purchaseOrder, String receiptKey, Long invoiceFileId, User receivedBy) {
        this.purchaseOrder = purchaseOrder;
        this.receiptKey = receiptKey;
        this.invoiceFileId = invoiceFileId;
        this.receivedBy = receivedBy;
        this.receivedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public PurchaseOrder getPurchaseOrder() { return purchaseOrder; }
    public void setPurchaseOrder(PurchaseOrder purchaseOrder) { this.purchaseOrder = purchaseOrder; }

    public String getReceiptKey() { return receiptKey; }
    public void setReceiptKey(String receiptKey) { this.receiptKey = receiptKey; }

    public Long getInvoiceFileId() { return invoiceFileId; }
    public void setInvoiceFileId(Long invoiceFileId) { this.invoiceFileId = invoiceFileId; }

    public Instant getReceivedAt() { return receivedAt; }
    public void setReceivedAt(Instant receivedAt) { this.receivedAt = receivedAt; }

    public User getReceivedBy() { return receivedBy; }
    public void setReceivedBy(User receivedBy) { this.receivedBy = receivedBy; }

    public List<GoodsReceiptItem> getItems() { return items; }
    public void setItems(List<GoodsReceiptItem> items) { this.items = items; }

    public void addItem(GoodsReceiptItem item) {
        items.add(item);
        item.setReceipt(this);
    }
}
