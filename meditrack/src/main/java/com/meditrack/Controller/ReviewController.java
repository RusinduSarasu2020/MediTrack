package com.meditrack.controller;

import com.meditrack.dto.ReviewForm;
import com.meditrack.model.User;
import com.meditrack.service.ReviewService;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final UserService userService;

    public ReviewController(ReviewService reviewService, UserService userService) {
        this.reviewService = reviewService;
        this.userService = userService;
    }

    @GetMapping
    public String listUserReviews(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("reviews", reviewService.getCustomerReviews(user.getId()));
        return "reviews/list";
    }

    @GetMapping("/new")
    public String newReviewForm(@RequestParam Long orderId, @RequestParam Long medicineId, Model model) {
        ReviewForm form = new ReviewForm();
        form.setOrderId(orderId);
        form.setMedicineId(medicineId);
        model.addAttribute("form", form);
        return "reviews/form";
    }

    @PostMapping
    public String saveReview(@Valid @ModelAttribute("form") ReviewForm form,
                             BindingResult errors,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "reviews/form";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            reviewService.addReview(form, user);
            redirectAttributes.addFlashAttribute("successMessage", "Review posted successfully!");
            return "redirect:/orders/" + form.getOrderId();
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/orders/" + form.getOrderId();
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteReview(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        try {
            reviewService.deleteReview(id, user);
            redirectAttributes.addFlashAttribute("successMessage", "Review deleted.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/profile";
    }
}
