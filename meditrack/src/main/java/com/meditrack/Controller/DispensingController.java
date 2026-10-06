package com.meditrack.Controller;

import com.meditrack.Model.enums.OrderStatus;
import com.meditrack.Model.CustomerOrder;
import com.meditrack.Model.User;
import com.meditrack.Repository.CustomerOrderRepository;
import com.meditrack.Service.PrescriptionService;
import com.meditrack.Service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Controller
@RequestMapping("/pharmacist")
public class DispensingController {

    private final CustomerOrderRepository orderRepository;
    private final PrescriptionService prescriptionService;
    private final UserService userService;

    public DispensingController(CustomerOrderRepository orderRepository, PrescriptionService prescriptionService, UserService userService) {
        this.orderRepository = orderRepository;
        this.prescriptionService = prescriptionService;
        this.userService = userService;
    }

    @GetMapping("/dispensing")
    public String list(Model model) {
        List<CustomerOrder> paidOrders = orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PAID);
        model.addAttribute("orders", paidOrders);
        return "dispensing/list";
    }

    @PostMapping("/dispensing/{id}")
    public String dispense(@PathVariable Long id,
                           @RequestParam(required = false, defaultValue = "") String notes,
                           Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        User pharmacist = userService.findByUsername(authentication.getName());
        try {
            prescriptionService.dispense(id, notes, pharmacist);
            redirectAttributes.addFlashAttribute("successMessage", "Dispensing record created. Order #" + id + " is ready for collection.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/pharmacist/dispensing";
    }
}
