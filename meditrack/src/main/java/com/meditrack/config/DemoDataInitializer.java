package com.meditrack.meditrack.config;

import com.meditrack.meditrack.model.User;
import com.meditrack.meditrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a couple of default login accounts on first startup so the module
 * can be demoed immediately without manual DB setup.
 *
 * Default accounts:
 *   username: coordinator  | password: coordinator123 | role: SUPPLIER_COORDINATOR
 *   username: admin        | password: admin123       | role: ADMIN
 */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("coordinator").isEmpty()) {
            User coordinator = new User();
            coordinator.setUsername("coordinator");
            coordinator.setPassword(passwordEncoder.encode("coordinator123"));
            coordinator.setFullName("Niroshan Rathnayake");
            coordinator.setRole(User.Role.SUPPLIER_COORDINATOR);
            coordinator.setEnabled(true);
            userRepository.save(coordinator);
        }

        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Yasara Fernando");
            admin.setRole(User.Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        }
    }
}
