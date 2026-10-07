package com.meditrack.service;

import com.meditrack.dto.CartItemDto;
import com.meditrack.enums.OrderChannel;
import com.meditrack.enums.OrderStatus;
import com.meditrack.exception.BusinessException;
import com.meditrack.exception.ResourceNotFoundException;
import com.meditrack.model.*;
import com.meditrack.repository.CustomerOrderRepository;
import com.meditrack.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final MedicineRepository medicineRepository;
    private final InventoryService inventoryService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public OrderService(CustomerOrderRepository orderRepository, MedicineRepository medicineRepository,
                        InventoryService inventoryService, AuditService auditService, NotificationService notificationService) {
        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
        this.inventoryService = inventoryService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CustomerOrder createOnlineOrder(User customer, List<CartItemDto> cartItems, Long prescriptionFileId) {
        if (cartItems.isEmpty()) {
            throw new BusinessException("Cart is empty.");
        }

        boolean requiresPrescription = false;
        BigDecimal quote = BigDecimal.ZERO;

        for (CartItemDto item : cartItems) {
            Medicine med = medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicineId()));
            if (!med.isActive()) {
                throw new BusinessException("Medicine " + med.getBrandName() + " is no longer available.");
            }
            if (med.isPrescriptionRequired()) {
                requiresPrescription = true;
            }
            int available = inventoryService.getSaleableStockCount(med.getId());
            if (available < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for " + med.getBrandName() + " (requested " + item.getQuantity() + ", available " + available + ")");
            }
            quote = quote.add(item.getSubtotal());
        }

        if (requiresPrescription && prescriptionFileId == null) {
            throw new BusinessException("Prescription upload is required for prescription-only medicines.");
        }

        OrderStatus initialStatus = requiresPrescription ? OrderStatus.PENDING_PRESCRIPTION : OrderStatus.AWAITING_PAYMENT;
        CustomerOrder order = new CustomerOrder(customer, null, OrderChannel.ONLINE, initialStatus);
        order.setQuoteTotal(quote);

        for (CartItemDto item : cartItems) {
            Medicine med = medicineRepository.findById(item.getMedicineId()).get();
            order.addItem(new OrderItem(order, med, item.getQuantity()));
        }

        if (requiresPrescription) {
            Prescription prescription = new Prescription(order, prescriptionFileId);
            for (CartItemDto item : cartItems) {
                Medicine med = medicineRepository.findById(item.getMedicineId()).get();
                if (med.isPrescriptionRequired()) {
                    prescription.addItem(new PrescriptionItem(prescription, med, item.getQuantity()));
                }
            }
            order.getPrescriptions().add(prescription);
        }

        CustomerOrder saved = orderRepository.save(order);
        auditService.log(customer.getId(), "CREATE_ORDER", "CustomerOrder", saved.getId(), "Created online order #" + saved.getId());
        notificationService.notify(customer.getId(), "Your order #" + saved.getId() + " has been placed. Status: " + initialStatus, "/orders/" + saved.getId());

        return saved;
    }

    @Transactional
    public CustomerOrder createInStoreOrder(User cashier, User customer, List<CartItemDto> items) {
        if (items.isEmpty()) {
            throw new BusinessException("No items selected for sale.");
        }

        boolean requiresPrescription = false;
        BigDecimal quote = BigDecimal.ZERO;

        for (CartItemDto item : items) {
            Medicine med = medicineRepository.findById(item.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + item.getMedicineId()));
            if (!med.isActive()) {
                throw new BusinessException("Medicine " + med.getBrandName() + " is inactive.");
            }
            if (med.isPrescriptionRequired()) {
                requiresPrescription = true;
            }
            quote = quote.add(item.getSubtotal());
        }

        if (requiresPrescription && customer == null) {
            throw new BusinessException("In-store prescription orders require a registered customer record.");
        }

        OrderStatus initialStatus = requiresPrescription ? OrderStatus.PENDING_PRESCRIPTION : OrderStatus.AWAITING_PAYMENT;
        CustomerOrder order = new CustomerOrder(customer, cashier, OrderChannel.IN_STORE, initialStatus);
        order.setQuoteTotal(quote);

        for (CartItemDto item : items) {
            Medicine med = medicineRepository.findById(item.getMedicineId()).get();
            order.addItem(new OrderItem(order, med, item.getQuantity()));
        }

        CustomerOrder saved = orderRepository.save(order);
        auditService.log(cashier.getId(), "CREATE_IN_STORE_ORDER", "CustomerOrder", saved.getId(), "Created in-store order #" + saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public CustomerOrder findOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findOrdersByCustomer(Long customerId) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<CustomerOrder> findOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public void cancelUnpaidOrder(Long orderId, User actor) {
        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PRESCRIPTION &&
            order.getStatus() != OrderStatus.AWAITING_PAYMENT &&
            order.getStatus() != OrderStatus.REJECTED) {
            throw new BusinessException("Cannot cancel order in status " + order.getStatus() + ". Use refund workflow for paid orders.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        auditService.log(actor.getId(), "CANCEL_ORDER", "CustomerOrder", order.getId(), "Order #" + order.getId() + " was cancelled.");
    }
}
