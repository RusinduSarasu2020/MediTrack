package com.meditrack.controller;

import com.meditrack.dto.PurchaseOrderForm;
import com.meditrack.model.PurchaseOrder;
import com.meditrack.model.User;
import com.meditrack.service.InventoryService;
import com.meditrack.service.ProcurementService;
import com.meditrack.service.SupplierService;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/purchase-orders")
public class PurchaseOrderController {

    private final ProcurementService procurementService;
    private final SupplierService supplierService;
    private final InventoryService inventoryService;
    private final UserService userService;

    public PurchaseOrderController(ProcurementService procurementService, SupplierService supplierService,
                                   InventoryService inventoryService, UserService userService) {
        this.procurementService = procurementService;
        this.supplierService = supplierService;
        this.inventoryService = inventoryService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", procurementService.findAllPurchaseOrders());
        return "purchase-orders/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new PurchaseOrderForm());
        model.addAttribute("suppliers", supplierService.findActive());
        model.addAttribute("medicines", inventoryService.findActiveMedicines());
        return "purchase-orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PurchaseOrderForm form,
                         BindingResult errors,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            model.addAttribute("suppliers", supplierService.findActive());
            model.addAttribute("medicines", inventoryService.findActiveMedicines());
            return "purchase-orders/form";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            PurchaseOrder po = procurementService.createPurchaseOrder(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Draft purchase order #" + po.getId() + " created.");
            return "redirect:/purchase-orders/" + po.getId();
        } catch (Exception ex) {
            errors.reject("poError", ex.getMessage());
            model.addAttribute("suppliers", supplierService.findActive());
            model.addAttribute("medicines", inventoryService.findActiveMedicines());
            return "purchase-orders/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        PurchaseOrder po = procurementService.findPurchaseOrderById(id);
        model.addAttribute("order", po);
        return "purchase-orders/detail";
    }

    @PostMapping("/{id}/confirm")
    public String confirm(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        procurementService.confirmPurchaseOrder(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Purchase order #" + id + " confirmed as ORDERED.");
        return "redirect:/purchase-orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        procurementService.cancelPurchaseOrder(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Purchase order #" + id + " cancelled.");
        return "redirect:/purchase-orders/" + id;
    }
}
