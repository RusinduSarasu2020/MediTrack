package com.meditrack.controller;

import com.meditrack.model.User;
import com.meditrack.service.NotificationService;
import com.meditrack.service.OrderService;
import com.meditrack.service.PrescriptionService;
import com.meditrack.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final UserService userService;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final PrescriptionService prescriptionService;

    public DashboardController(UserService userService, OrderService orderService, NotificationService notificationService,
                               PrescriptionService prescriptionService) {
        this.userService = userService;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.prescriptionService = prescriptionService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }

        User user = userService.findByUsername(authentication.getName());
        return switch (user.getRole()) {
            case ADMIN -> "redirect:/admin/dashboard";
            case PHARMACIST -> "redirect:/pharmacist/dashboard";
            case CASHIER -> "redirect:/cashier/dashboard";
            case SUPPLIER_COORDINATOR -> "redirect:/supplier/dashboard";
            case CUSTOMER -> "redirect:/customer/dashboard";
            case OWNER -> "redirect:/owner/dashboard";
        };
    }

    @GetMapping("/customer/dashboard")
    public String customerDashboard(Authentication authentication, org.springframework.ui.Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("user", user);
        model.addAttribute("recentOrders", orderService.findOrdersByCustomer(user.getId()));
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
        model.addAttribute("prescriptions", prescriptionService.findForCustomer(user.getId()));
        return "customer/dashboard";
    }
}
