package com.meditrack.meditrack.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a supplier / vendor who provides medicines to the pharmacy.
 * Managed by the Supplier Coordinator.
 */
@Entity
@Table(name = "suppliers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long supplierId;

    @NotBlank(message = "Company name is required")
    @Column(nullable = false)
    private String companyName;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @Email(message = "Enter a valid email")
    @NotBlank(message = "Email is required")
    private String email;

    @Column(length = 500)
    private String address;

    /** Comma separated list of medicines supplied, e.g. "Paracetamol, Amoxicillin" */
    @Column(length = 1000)
    private String medicinesSupplied;

    private String paymentTerms; // e.g. "Net 30", "Cash on Delivery"

    @Enumerated(EnumType.STRING)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrder> purchaseOrders = new ArrayList<>();

    public enum SupplierStatus {
        ACTIVE, INACTIVE
    }
}
