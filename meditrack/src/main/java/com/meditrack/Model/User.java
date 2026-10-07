package com.meditrack.meditrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Basic application user for authentication and role-based access control.
 * Roles map to the six MediTrack stakeholder personas.
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password; // stored BCrypt-encoded

    @NotBlank
    private String fullName;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean enabled = true;

    public enum Role {
        ADMIN,
        PHARMACY_OWNER,
        SUPPLIER_COORDINATOR,
        SENIOR_PHARMACIST,
        CASHIER,
        CUSTOMER
    }
}
