package com.meditrack.meditrack.controller;

import com.meditrack.meditrack.model.PurchaseOrder;
import com.meditrack.meditrack.model.PurchaseOrderItem;
import com.meditrack.meditrack.model.Supplier;
import com.meditrack.meditrack.service.PurchaseOrderService;
import com.meditrack.meditrack.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Handles Purchase Order creation, approval workflow, and delivery tracking.
 * Covers PB-09 (view/process auto-generated POs) and PB-12 (update delivery status).
 */
@Controller
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;
    private final SupplierService supplierService;

    @GetMapping
    public String listOrders(@RequestParam(required = false) String keyword,
                              @RequestParam(required = false) PurchaseOrder.OrderStatus status,
                              Model model) {
        if (status != null) {
            model.addAttribute("orders", purchaseOrderService.getOrdersByStatus(status));
        } else {
            model.addAttribute("orders", purchaseOrderService.searchByReference(keyword));
        }
        model.addAttribute("keyword", keyword);
        model.addAttribute("statuses", PurchaseOrder.OrderStatus.values());
        model.addAttribute("selectedStatus", status);
        return "purchaseorder/list";
    }

    @GetMapping("/new")
    public String newOrderForm(Model model) {
        PurchaseOrder order = new PurchaseOrder();
        order.getItems().add(new PurchaseOrderItem());
        model.addAttribute("order", order);
        model.addAttribute("suppliers", supplierService.getActiveSuppliers());
        return "purchaseorder/form";
    }

    @PostMapping("/save")
    public String saveOrder(@ModelAttribute("order") PurchaseOrder order,
                             @RequestParam Long supplierId) {
        Supplier supplier = supplierService.getSupplierById(supplierId);
        order.setSupplier(supplier);

        // Remove any blank line items submitted from the dynamic form
        order.getItems().removeIf(item ->
                item.getMedicineName() == null || item.getMedicineName().isBlank());

        if (order.getOrderId() == null) {
            purchaseOrderService.createOrder(order);
        } else {
            purchaseOrderService.updateOrder(order);
        }
        return "redirect:/purchase-orders";
    }

    @GetMapping("/{id}")
    public String viewOrder(@PathVariable Long id, Model model) {
        model.addAttribute("order", purchaseOrderService.getOrderById(id));
        model.addAttribute("statuses", PurchaseOrder.OrderStatus.values());
        return "purchaseorder/view";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id,
                                @RequestParam PurchaseOrder.OrderStatus status,
                                @AuthenticationPrincipal UserDetails userDetails) {
        String approvedBy = userDetails != null ? userDetails.getUsername() : "system";
        purchaseOrderService.updateStatus(id, status, approvedBy);
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/delivery")
    public String updateDelivery(@PathVariable Long id,
                                  @RequestParam(required = false)
                                  @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
                                  LocalDate expectedDeliveryDate,
                                  @RequestParam(required = false) String deliveryNotes) {
        purchaseOrderService.updateDeliveryInfo(id, expectedDeliveryDate, deliveryNotes);
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        purchaseOrderService.cancelOrder(id);
        return "redirect:/purchase-orders/" + id;
    }
}
