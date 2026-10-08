package com.meditrack.controller;

import com.meditrack.model.Notification;
import com.meditrack.model.User;
import com.meditrack.service.NotificationService;
import com.meditrack.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.Optional;

/** In-system notifications. Every action is scoped to the signed-in user's own notifications. */
@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping({"/notifications", "/customer/notifications"})
    public String list(Authentication authentication, Model model) {
        User user = userService.findByUsername(authentication.getName());
        model.addAttribute("notifications", notificationService.getNotificationsForUser(user.getId()));
        return "notifications/list";
    }

    @PostMapping("/notifications/{id}/read")
    public String markAsRead(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        notificationService.markAsRead(id, user.getId());
        return "redirect:/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(authentication.getName());
        int updated = notificationService.markAllAsRead(user.getId());
        redirectAttributes.addFlashAttribute("successMessage",
                updated == 0 ? "No unread notifications." : updated + " notification(s) marked as read.");
        return "redirect:/notifications";
    }

    /** Marks the notification read and follows its link (internal paths only). */
    @PostMapping("/notifications/{id}/open")
    public String open(@PathVariable Long id, Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        Optional<Notification> notification = notificationService.openForUser(id, user.getId());
        String link = notification.map(Notification::getLink).orElse(null);
        if (link != null && link.startsWith("/") && !link.startsWith("//")) {
            return "redirect:" + link;
        }
        return "redirect:/notifications";
    }
}
