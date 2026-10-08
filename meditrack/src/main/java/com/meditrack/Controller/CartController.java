package com.meditrack.controller;

import com.meditrack.service.CartService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("subtotal", cartService.getSubtotal());
        model.addAttribute("requiresPrescription", cartService.hasPrescriptionItems());
        return "cart/view";
    }

    @PostMapping("/add")
    public String addItem(@RequestParam Long medicineId,
                          @RequestParam(defaultValue = "1") int quantity) {
        cartService.addItem(medicineId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String updateItem(@RequestParam Long medicineId,
                             @RequestParam int quantity) {
        cartService.updateItem(medicineId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String removeItem(@RequestParam Long medicineId) {
        cartService.removeItem(medicineId);
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart() {
        cartService.clear();
        return "redirect:/cart";
    }
}
