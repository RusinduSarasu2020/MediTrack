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
    public String createForm(@RequestParam(required = false) Long supplierId, Model model) {
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.setSupplierId(supplierId);
        model.addAttribute("form", form);
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

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        PurchaseOrder po = procurementService.findPurchaseOrderById(id);
        if (po.getStatus() != com.meditrack.enums.PurchaseOrderStatus.DRAFT) {
            redirectAttributes.addFlashAttribute("errorMessage", "Only DRAFT purchase orders can be edited.");
            return "redirect:/purchase-orders/" + id;
        }
        PurchaseOrderForm form = new PurchaseOrderForm();
        form.setId(po.getId());
        form.setSupplierId(po.getSupplier().getId());
        form.setExpectedDate(po.getExpectedDate());
        form.setNotes(po.getNotes());
        for (var item : po.getItems()) {
            PurchaseOrderForm.ItemRow row = new PurchaseOrderForm.ItemRow();
            row.setMedicineId(item.getMedicine().getId());
            row.setQuantity(item.getOrderedQty());
            row.setUnitCost(item.getUnitCost());
            form.getItems().add(row);
        }
        model.addAttribute("form", form);
        model.addAttribute("suppliers", supplierService.findActive());
        model.addAttribute("medicines", inventoryService.findActiveMedicines());
        return "purchase-orders/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PurchaseOrderForm form,
                         BindingResult errors,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        form.setId(id);
        if (!errors.hasErrors()) {
            User user = userService.findByUsername(authentication.getName());
            try {
                procurementService.updateDraftPurchaseOrder(id, form, user.getId());
                redirectAttributes.addFlashAttribute("successMessage", "Purchase order #" + id + " updated.");
                return "redirect:/purchase-orders/" + id;
            } catch (Exception ex) {
                errors.reject("poError", ex.getMessage());
            }
        }
        model.addAttribute("suppliers", supplierService.findActive());
        model.addAttribute("medicines", inventoryService.findActiveMedicines());
        return "purchase-orders/form";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        try {
            procurementService.deletePurchaseOrder(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Purchase order #" + id + " deleted.");
            return "redirect:/purchase-orders";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/purchase-orders/" + id;
        }
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
