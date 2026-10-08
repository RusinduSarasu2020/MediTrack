package com.meditrack.controller;

import com.meditrack.dto.SupplierForm;
import com.meditrack.exception.FieldValidationException;
import com.meditrack.model.Supplier;
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
public class SupplierController {

    private final SupplierService supplierService;
    private final ProcurementService procurementService;
    private final UserService userService;
    private final InventoryService inventoryService;

    public SupplierController(SupplierService supplierService, ProcurementService procurementService, UserService userService,
                              InventoryService inventoryService) {
        this.supplierService = supplierService;
        this.procurementService = procurementService;
        this.userService = userService;
        this.inventoryService = inventoryService;
    }

    @GetMapping("/supplier/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("suppliers", supplierService.findActive());
        model.addAttribute("recentOrders", procurementService.findAllPurchaseOrders());
        return "supplier/dashboard";
    }

    @GetMapping("/suppliers")
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "suppliers/list";
    }

    @GetMapping("/suppliers/new")
    public String createForm(Model model) {
        model.addAttribute("form", new SupplierForm());
        return "suppliers/form";
    }

    @PostMapping("/suppliers")
    public String create(@Valid @ModelAttribute("form") SupplierForm form,
                         BindingResult errors,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "suppliers/form";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            Supplier saved = supplierService.create(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Supplier registered successfully.");
            return "redirect:/suppliers/" + saved.getId();
        } catch (FieldValidationException ex) {
            errors.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "suppliers/form";
        }
    }

    @GetMapping("/suppliers/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Supplier supplier = supplierService.findById(id);
        model.addAttribute("supplier", supplier);
        model.addAttribute("orders", procurementService.findPurchaseOrdersBySupplier(id));
        model.addAttribute("batches", inventoryService.findBatchesBySupplier(id));
        model.addAttribute("referenceCount", supplierService.countReferences(id));
        return "suppliers/detail";
    }

    @GetMapping("/suppliers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Supplier s = supplierService.findById(id);
        SupplierForm form = new SupplierForm();
        form.setId(s.getId());
        form.setCompanyName(s.getCompanyName());
        form.setContactPerson(s.getContactPerson());
        form.setEmail(s.getEmail());
        form.setPhone(s.getPhone());
        form.setAddress(s.getAddress());
        form.setActive(s.isActive());
        model.addAttribute("form", form);
        return "suppliers/form";
    }

    @PostMapping("/suppliers/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") SupplierForm form,
                         BindingResult errors,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "suppliers/form";
        }
        form.setId(id);
        User user = userService.findByUsername(authentication.getName());
        try {
            supplierService.update(id, form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Supplier updated successfully.");
            return "redirect:/suppliers/" + id;
        } catch (FieldValidationException ex) {
            errors.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "suppliers/form";
        }
    }

    @PostMapping("/suppliers/{id}/deactivate")
    public String deactivate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        supplierService.deactivate(id, user.getId());
        redirectAttributes.addFlashAttribute("successMessage", "Supplier deactivated.");
        return "redirect:/suppliers";
    }

    @PostMapping("/suppliers/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        try {
            supplierService.delete(id, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Supplier deleted successfully from directory.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/suppliers";
    }
}
