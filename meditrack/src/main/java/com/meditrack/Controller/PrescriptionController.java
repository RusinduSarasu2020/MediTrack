package com.meditrack.Controller;

import com.meditrack.Model.dto.PrescriptionReviewForm;
import com.meditrack.Model.enums.OrderStatus;
import com.meditrack.Model.enums.PrescriptionStatus;
import com.meditrack.Model.Prescription;
import com.meditrack.Model.User;
import com.meditrack.Repository.CustomerOrderRepository;
import com.meditrack.Service.InventoryService;
import com.meditrack.Service.PrescriptionService;
import com.meditrack.Service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/pharmacist")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final CustomerOrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final UserService userService;

    public PrescriptionController(PrescriptionService prescriptionService, CustomerOrderRepository orderRepository,
                                  InventoryService inventoryService, UserService userService) {
        this.prescriptionService = prescriptionService;
        this.orderRepository = orderRepository;
        this.inventoryService = inventoryService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("pendingPrescriptions", prescriptionService.findPendingPrescriptions());
        model.addAttribute("awaitingDispensing", orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PAID));
        model.addAttribute("lowStockMedicines", inventoryService.getLowStockMedicines());
        return "pharmacist/dashboard";
    }

    @GetMapping("/prescriptions")
    public String list(Model model) {
        model.addAttribute("prescriptions", prescriptionService.findPendingPrescriptions());
        return "prescriptions/list";
    }

    @GetMapping("/prescriptions/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Prescription rx = prescriptionService.findById(id);
        PrescriptionReviewForm form = new PrescriptionReviewForm();
        form.setPrescriptionId(rx.getId());
        form.setStatus(PrescriptionStatus.APPROVED);

        model.addAttribute("prescription", rx);
        model.addAttribute("form", form);
        return "prescriptions/detail";
    }

    @PostMapping("/prescriptions/{id}/review")
    public String review(@PathVariable Long id,
                         @Valid @ModelAttribute("form") PrescriptionReviewForm form,
                         BindingResult errors,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        User pharmacist = userService.findByUsername(authentication.getName());
        try {
            prescriptionService.reviewPrescription(form, pharmacist);
            redirectAttributes.addFlashAttribute("successMessage", "Prescription decision recorded as " + form.getStatus());
            return "redirect:/pharmacist/prescriptions";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/pharmacist/prescriptions/" + id;
        }
    }
}
