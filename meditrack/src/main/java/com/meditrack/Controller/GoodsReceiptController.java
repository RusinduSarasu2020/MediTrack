package com.meditrack.controller;

import com.meditrack.dto.GoodsReceiptForm;
import com.meditrack.model.PurchaseOrder;
import com.meditrack.model.User;
import com.meditrack.service.ProcurementService;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/receipts")
public class GoodsReceiptController {

    private final ProcurementService procurementService;
    private final UserService userService;

    public GoodsReceiptController(ProcurementService procurementService, UserService userService) {
        this.procurementService = procurementService;
        this.userService = userService;
    }

    @GetMapping("/new")
    public String createForm(@RequestParam Long purchaseOrderId, Model model) {
        PurchaseOrder po = procurementService.findPurchaseOrderById(purchaseOrderId);
        GoodsReceiptForm form = new GoodsReceiptForm();
        form.setPurchaseOrderId(po.getId());
        form.setReceiptKey("REC-" + po.getId() + "-" + System.currentTimeMillis());

        model.addAttribute("order", po);
        model.addAttribute("form", form);
        return "receipts/forms";
    }

    @PostMapping
    public String receive(@Valid @ModelAttribute("form") GoodsReceiptForm form,
                          BindingResult errors,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (errors.hasErrors()) {
            PurchaseOrder po = procurementService.findPurchaseOrderById(form.getPurchaseOrderId());
            model.addAttribute("order", po);
            return "receipts/forms";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            procurementService.receiveGoods(form, user.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Goods receipt recorded and inventory batches updated successfully.");
            return "redirect:/purchase-orders/" + form.getPurchaseOrderId();
        } catch (Exception ex) {
            errors.reject("receiptError", ex.getMessage());
            PurchaseOrder po = procurementService.findPurchaseOrderById(form.getPurchaseOrderId());
            model.addAttribute("order", po);
            return "receipts/forms";
        }
    }
}
