package com.meditrack.service;

import com.meditrack.dto.GoodsReceiptForm;
import com.meditrack.dto.PurchaseOrderForm;
import com.meditrack.enums.PurchaseOrderStatus;
import com.meditrack.enums.StockMovementType;
import com.meditrack.exception.BusinessException;
import com.meditrack.exception.ResourceNotFoundException;
import com.meditrack.model.*;
import com.meditrack.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class ProcurementService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GoodsReceiptRepository goodsReceiptRepository;
    private final SupplierRepository supplierRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final Clock clock;

    public ProcurementService(PurchaseOrderRepository purchaseOrderRepository,
                              PurchaseOrderItemRepository purchaseOrderItemRepository,
                              GoodsReceiptRepository goodsReceiptRepository,
                              SupplierRepository supplierRepository,
                              MedicineRepository medicineRepository,
                              MedicineBatchRepository batchRepository,
                              StockMovementRepository stockMovementRepository,
                              UserRepository userRepository,
                              AuditService auditService,
                              Clock clock) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.supplierRepository = supplierRepository;
        this.medicineRepository = medicineRepository;
        this.batchRepository = batchRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findAllPurchaseOrders() {
        return purchaseOrderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PurchaseOrder findPurchaseOrderById(Long id) {
        return purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrder> findPurchaseOrdersBySupplier(Long supplierId) {
        return purchaseOrderRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId);
    }

    @Transactional
    public PurchaseOrder createPurchaseOrder(PurchaseOrderForm form, Long actorId) {
        Supplier supplier = activeSupplier(form.getSupplierId());
        validateExpectedDate(form);
        User actor = userRepository.findById(actorId).orElse(null);

        PurchaseOrder po = new PurchaseOrder(supplier, form.getExpectedDate(), actor);
        po.setNotes(blankToNull(form.getNotes()));
        applyItems(po, form);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditService.log(actorId, "CREATE_PURCHASE_ORDER", "PurchaseOrder", saved.getId(), "Created PO #" + saved.getId());
        return saved;
    }

    /** Draft purchase orders can be fully edited (supplier, date, notes, lines) until they are sent. */
    @Transactional
    public PurchaseOrder updateDraftPurchaseOrder(Long orderId, PurchaseOrderForm form, Long actorId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + orderId));
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be edited. PO #" + orderId + " is " + po.getStatus() + ".");
        }
        po.setSupplier(activeSupplier(form.getSupplierId()));
        validateExpectedDate(form);
        po.setExpectedDate(form.getExpectedDate());
        po.setNotes(blankToNull(form.getNotes()));
        po.getItems().clear();          // orphanRemoval deletes the old lines
        applyItems(po, form);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        auditService.log(actorId, "UPDATE_PURCHASE_ORDER", "PurchaseOrder", saved.getId(),
                "Updated draft PO #" + saved.getId() + " (" + saved.getItems().size() + " line(s))");
        return saved;
    }

    /** Deletes a DRAFT or CANCELLED purchase order that never received goods; others must be kept for the stock trail. */
    @Transactional
    public void deletePurchaseOrder(Long orderId, Long actorId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + orderId));
        if (po.getStatus() != PurchaseOrderStatus.DRAFT && po.getStatus() != PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException("Only DRAFT or CANCELLED purchase orders can be deleted. Cancel PO #" + orderId + " instead.");
        }
        if (goodsReceiptRepository.countByPurchaseOrderId(orderId) > 0) {
            throw new BusinessException("PO #" + orderId + " has goods receipts recorded against it and must be kept.");
        }
        String summary = "Deleted " + po.getStatus() + " PO #" + orderId + " for " + po.getSupplier().getCompanyName();
        purchaseOrderRepository.delete(po);
        purchaseOrderRepository.flush();
        auditService.log(actorId, "DELETE_PURCHASE_ORDER", "PurchaseOrder", orderId, summary);
    }

    private Supplier activeSupplier(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + supplierId));
        if (!supplier.isActive()) {
            throw new BusinessException("Cannot create purchase order for inactive supplier.");
        }
        return supplier;
    }

    private void validateExpectedDate(PurchaseOrderForm form) {
        if (form.getExpectedDate() != null && form.getExpectedDate().isBefore(LocalDate.now(clock))) {
            throw new BusinessException("Expected delivery date cannot be in the past.");
        }
    }

    private void applyItems(PurchaseOrder po, PurchaseOrderForm form) {
        if (form.getItems() != null) {
            for (PurchaseOrderForm.ItemRow itemRow : form.getItems()) {
                if (itemRow.getMedicineId() != null && itemRow.getQuantity() != null && itemRow.getQuantity() > 0) {
                    Medicine medicine = medicineRepository.findById(itemRow.getMedicineId())
                            .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + itemRow.getMedicineId()));
                    BigDecimal cost = itemRow.getUnitCost() != null ? itemRow.getUnitCost() : BigDecimal.ZERO;
                    if (cost.signum() < 0) {
                        throw new BusinessException("Unit cost cannot be negative.");
                    }
                    po.addItem(new PurchaseOrderItem(po, medicine, itemRow.getQuantity(), cost));
                }
            }
        }
        if (po.getItems().isEmpty()) {
            throw new BusinessException("Purchase order must contain at least one medicine item with quantity greater than 0.");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    @Transactional
    public void confirmPurchaseOrder(Long orderId, Long actorId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + orderId));
        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new BusinessException("Only DRAFT purchase orders can be ordered.");
        }
        po.setStatus(PurchaseOrderStatus.ORDERED);
        purchaseOrderRepository.save(po);
        auditService.log(actorId, "CONFIRM_PURCHASE_ORDER", "PurchaseOrder", po.getId(), "PO #" + po.getId() + " marked as ORDERED");
    }

    @Transactional
    public void cancelPurchaseOrder(Long orderId, Long actorId) {
        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + orderId));
        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BusinessException("Cannot cancel already received purchase order.");
        }
        po.setStatus(PurchaseOrderStatus.CANCELLED);
        purchaseOrderRepository.save(po);
        auditService.log(actorId, "CANCEL_PURCHASE_ORDER", "PurchaseOrder", po.getId(), "PO #" + po.getId() + " CANCELLED");
    }

    @Transactional
    public GoodsReceipt receiveGoods(GoodsReceiptForm form, Long actorId) {
        // Idempotency check: if receipt key exists, return existing receipt
        if (goodsReceiptRepository.existsByReceiptKey(form.getReceiptKey())) {
            return goodsReceiptRepository.findByReceiptKey(form.getReceiptKey()).get();
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdForUpdate(form.getPurchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Purchase Order not found: " + form.getPurchaseOrderId()));

        if (po.getStatus() != PurchaseOrderStatus.ORDERED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessException("Goods can only be received for ORDERED or PARTIALLY_RECEIVED orders.");
        }

        User actor = userRepository.findById(actorId).orElse(null);
        GoodsReceipt receipt = new GoodsReceipt(po, form.getReceiptKey(), null, actor);
        LocalDate today = LocalDate.now(clock);

        for (GoodsReceiptForm.ItemRow itemRow : form.getItems()) {
            if (itemRow.getQuantity() == null || itemRow.getQuantity() <= 0) {
                continue;
            }
            PurchaseOrderItem poi = purchaseOrderItemRepository.findById(itemRow.getPurchaseOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Purchase order item not found: " + itemRow.getPurchaseOrderItemId()));

            int outstanding = poi.getOrderedQty() - poi.getReceivedQty();
            if (itemRow.getQuantity() > outstanding) {
                throw new BusinessException("Cannot receive " + itemRow.getQuantity() + " units. Outstanding quantity is " + outstanding);
            }

            if (itemRow.getExpiryDate() == null || itemRow.getExpiryDate().isBefore(today) || itemRow.getExpiryDate().isEqual(today)) {
                throw new BusinessException("Delivered goods must have a future expiry date (after " + today + ")");
            }

            // Create batch
            MedicineBatch batch = new MedicineBatch(poi.getMedicine(), po.getSupplier(), itemRow.getBatchNumber(),
                    itemRow.getExpiryDate(), itemRow.getCostPrice(), itemRow.getSellingPrice(), itemRow.getQuantity());
            MedicineBatch savedBatch = batchRepository.save(batch);

            // Record stock movement
            StockMovement movement = new StockMovement(savedBatch, itemRow.getQuantity(), StockMovementType.RECEIPT,
                    "Goods receipt for PO #" + po.getId(), form.getReceiptKey(), actor);
            stockMovementRepository.save(movement);

            // Update PO item received quantity
            poi.setReceivedQty(poi.getReceivedQty() + itemRow.getQuantity());
            purchaseOrderItemRepository.save(poi);

            // Link to receipt
            receipt.addItem(new GoodsReceiptItem(receipt, poi, savedBatch, itemRow.getQuantity()));
        }

        GoodsReceipt savedReceipt = goodsReceiptRepository.save(receipt);

        // Update PO status
        boolean allComplete = true;
        for (PurchaseOrderItem item : po.getItems()) {
            if (item.getReceivedQty() < item.getOrderedQty()) {
                allComplete = false;
                break;
            }
        }
        if (allComplete) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
            po.setCompletedAt(Instant.now());
        } else {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        purchaseOrderRepository.save(po);

        auditService.log(actorId, "RECEIVE_GOODS", "GoodsReceipt", savedReceipt.getId(),
                "Received delivery with key: " + form.getReceiptKey() + " for PO #" + po.getId());

        return savedReceipt;
    }
}
