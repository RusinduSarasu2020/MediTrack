package com.meditrack.meditrack.controller;

import com.meditrack.meditrack.model.PurchaseOrder;
import com.meditrack.meditrack.model.Supplier;
import com.meditrack.meditrack.service.PurchaseOrderService;
import com.meditrack.meditrack.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * Handles the Supplier Directory: create, view, search, update, deactivate suppliers.
 * Corresponds to Supplier Coordinator user stories PB-09/PB-12 support functions.
 */
@Controller
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public String listSuppliers(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("suppliers", supplierService.searchByCompanyName(keyword));
        model.addAttribute("keyword", keyword);
        return "supplier/list";
    }

    @GetMapping("/new")
    public String newSupplierForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "supplier/form";
    }

    @GetMapping("/{id}/edit")
    public String editSupplierForm(@PathVariable Long id, Model model) {
        model.addAttribute("supplier", supplierService.getSupplierById(id));
        return "supplier/form";
    }

    @PostMapping("/save")
    public String saveSupplier(@Valid @ModelAttribute("supplier") Supplier supplier,
                                BindingResult result, Model model) {
        if (result.hasErrors()) {
            return "supplier/form";
        }
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers";
    }

    @GetMapping("/{id}")
    public String viewSupplier(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.getSupplierById(id);
        model.addAttribute("supplier", supplier);

        // Supplier performance snapshot
        double onTimeRate = purchaseOrderService.calculateOnTimeDeliveryRate(supplier);
        long totalOrders = purchaseOrderService.countTotalOrders(supplier);
        model.addAttribute("onTimeRate", onTimeRate);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("orders", purchaseOrderService.getOrdersBySupplier(supplier));

        return "supplier/view";
    }

    @PostMapping("/{id}/delete")
    public String deleteSupplier(@PathVariable Long id) {
        supplierService.deleteSupplier(id);
        return "redirect:/suppliers";
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id) {
        Supplier supplier = supplierService.getSupplierById(id);
        supplier.setStatus(supplier.getStatus() == Supplier.SupplierStatus.ACTIVE
                ? Supplier.SupplierStatus.INACTIVE
                : Supplier.SupplierStatus.ACTIVE);
        supplierService.saveSupplier(supplier);
        return "redirect:/suppliers/" + id;
    }
}
