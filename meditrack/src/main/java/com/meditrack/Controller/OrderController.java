package com.meditrack.controller;

import com.meditrack.dto.CartItemDto;
import com.meditrack.enums.OrderStatus;
import com.meditrack.enums.Role;
import com.meditrack.exception.BusinessException;
import com.meditrack.model.CustomerOrder;
import com.meditrack.model.StoredFile;
import com.meditrack.model.User;
import com.meditrack.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;
    private final FileStorageService fileStorageService;
    private final UserService userService;

    public OrderController(OrderService orderService, CartService cartService,
                           FileStorageService fileStorageService, UserService userService) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.fileStorageService = fileStorageService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        if (user.getRole() == Role.CUSTOMER) {
            model.addAttribute("orders", orderService.findOrdersByCustomer(user.getId()));
        } else {
            model.addAttribute("orders", orderService.findAllOrders());
        }
        return "orders/list";
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model) {
        if (cartService.isEmpty()) {
            return "redirect:/cart";
        }
        model.addAttribute("items", cartService.getItems());
        model.addAttribute("subtotal", cartService.getSubtotal());
        model.addAttribute("requiresPrescription", cartService.hasPrescriptionItems());
        return "orders/checkout";
    }

    @PostMapping("/checkout")
    public String submitOrder(@RequestParam(required = false) MultipartFile prescriptionFile,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (cartService.isEmpty()) {
            return "redirect:/cart";
        }
        User customer = userService.findByUsername(authentication.getName());
        Long fileId = null;

        if (cartService.hasPrescriptionItems()) {
            if (prescriptionFile == null || prescriptionFile.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "A prescription document upload is required for prescription medications.");
                return "redirect:/orders/checkout";
            }
            try {
                StoredFile stored = fileStorageService.store(prescriptionFile, customer.getId(), "PRESCRIPTION");
                fileId = stored.getId();
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
                return "redirect:/orders/checkout";
            }
        }

        try {
            CustomerOrder order = orderService.createOnlineOrder(customer, cartService.getItems(), fileId);
            cartService.clear();

            if (order.getStatus() == OrderStatus.PENDING_PRESCRIPTION) {
                redirectAttributes.addFlashAttribute("successMessage", "Order #" + order.getId() + " placed! Our pharmacist will review your prescription before payment.");
                return "redirect:/orders/" + order.getId();
            } else {
                return "redirect:/payment/" + order.getId();
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/checkout";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        CustomerOrder order = orderService.findOrderById(id);
        User user = userService.findByUsername(authentication.getName());

        // Authorization check: customer must own order or user is staff
        if (user.getRole() == Role.CUSTOMER && (order.getCustomer() == null || !order.getCustomer().getId().equals(user.getId()))) {
            throw new BusinessException("Access denied: You do not have permission to view this order.");
        }

        model.addAttribute("order", order);
        return "orders/detail";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        CustomerOrder order = orderService.findOrderById(id);

        if (user.getRole() == Role.CUSTOMER && (order.getCustomer() == null || !order.getCustomer().getId().equals(user.getId()))) {
            throw new BusinessException("Access denied.");
        }

        try {
            orderService.cancelUnpaidOrder(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Order #" + id + " has been cancelled.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }
}
