package com.meditrack.controller;

import com.meditrack.dto.RegisterForm;
import com.meditrack.exception.BusinessException;
import com.meditrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult errors,
                           RedirectAttributes redirectAttributes) {
        if (errors.hasErrors()) {
            return "auth/register";
        }
        try {
            userService.registerCustomer(form);
            redirectAttributes.addFlashAttribute("successMessage", "Account created successfully! Please sign in.");
            return "redirect:/login";
        } catch (BusinessException ex) {
            errors.reject("registrationError", ex.getMessage());
            return "auth/register";
        }
    }
}
