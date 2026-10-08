package com.meditrack.Model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dispense_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = "order_id")
})
public class DispenseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private CustomerOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacist_id", nullable = false)
    private User pharmacist;

    @Column(nullable = false)
    private Instant dispensedAt = Instant.now();

    @Column(length = 500)
    private String notes;

    public DispenseRecord() {}

    public DispenseRecord(CustomerOrder order, User pharmacist, String notes) {
        this.order = order;
        this.pharmacist = pharmacist;
        this.notes = notes;
        this.dispensedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public CustomerOrder getOrder() { return order; }
    public void setOrder(CustomerOrder order) { this.order = order; }

    public User getPharmacist() { return pharmacist; }
    public void setPharmacist(User pharmacist) { this.pharmacist = pharmacist; }

    public Instant getDispensedAt() { return dispensedAt; }
    public void setDispensedAt(Instant dispensedAt) { this.dispensedAt = dispensedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
