package com.meditrack.controller;

import com.meditrack.dto.ProfileForm;
import com.meditrack.exception.BusinessException;
import com.meditrack.model.User;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserService userService;

    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public String editProfile(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        ProfileForm form = new ProfileForm();
        form.setFullName(user.getFullName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());
        form.setAddress(user.getAddress());
        model.addAttribute("form", form);
        return "profile/edit";
    }

    @PostMapping
    public String updateProfile(Authentication authentication,
                                @Valid @ModelAttribute("form") ProfileForm form,
                                BindingResult errors,
                                RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "profile/edit";
        }
        User user = userService.findByUsername(authentication.getName());
        try {
            userService.updateProfile(user.getId(), form);
            redirectAttributes.addFlashAttribute("successMessage", "Profile updated successfully.");
            return "redirect:/profile";
        } catch (BusinessException ex) {
            errors.reject("profileError", ex.getMessage());
            return "profile/edit";
        }
    }
}
