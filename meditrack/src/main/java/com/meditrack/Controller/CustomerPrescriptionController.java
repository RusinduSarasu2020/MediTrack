package com.meditrack.controller;

import com.meditrack.dto.PrescriptionSubmissionForm;
import com.meditrack.exception.BusinessException;
import com.meditrack.model.Prescription;
import com.meditrack.model.User;
import com.meditrack.service.FileStorageService;
import com.meditrack.service.NotificationService;
import com.meditrack.service.PrescriptionService;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Customer-facing prescription pages. The customer is always the signed-in account; every lookup is
 * scoped to that customer, so one customer can never see or submit for another.
 */
@Controller
@RequestMapping("/customer/prescriptions")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPrescriptionController {

    private final PrescriptionService prescriptionService;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final UserService userService;

    public CustomerPrescriptionController(PrescriptionService prescriptionService, FileStorageService fileStorageService,
                                          NotificationService notificationService, UserService userService) {
        this.prescriptionService = prescriptionService;
        this.fileStorageService = fileStorageService;
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Authentication authentication, Model model) {
        User customer = userService.findByUsername(authentication.getName());
        model.addAttribute("prescriptions", prescriptionService.findForCustomer(customer.getId()));
        return "customer/prescriptions/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new PrescriptionSubmissionForm());
        return "customer/prescriptions/form";
    }

    @PostMapping
    public String submit(@Valid @ModelAttribute("form") PrescriptionSubmissionForm form,
                         BindingResult errors,
                         @RequestParam(value = "prescriptionFile", required = false) MultipartFile prescriptionFile,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (prescriptionFile == null || prescriptionFile.isEmpty()) {
            errors.reject("fileRequired", "Please choose a prescription file (JPG, PNG or PDF, up to 5 MB).");
        }
        if (errors.hasErrors()) {
            return "customer/prescriptions/form";
        }
        User customer = userService.findByUsername(authentication.getName());
        try {
            Prescription saved = prescriptionService.submitPrescription(customer, form, prescriptionFile);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Prescription #" + saved.getId() + " submitted. You'll be notified as a pharmacist reviews it.");
            return "redirect:/customer/prescriptions/" + saved.getId();
        } catch (BusinessException ex) {
            errors.reject("submitError", ex.getMessage());
            return "customer/prescriptions/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        User customer = userService.findByUsername(authentication.getName());
        Prescription rx = prescriptionService.findForCustomer(id, customer.getId());
        model.addAttribute("prescription", rx);
        model.addAttribute("document", rx.getFileId() != null ? fileStorageService.getMetadata(rx.getFileId()) : null);
        model.addAttribute("history", notificationService.getForPrescription(rx.getId()));
        return "customer/prescriptions/detail";
    }
}
