package com.meditrack.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class SupplierForm {

    private Long id;

    @NotBlank(message = "Company name is required.")
    @Size(max = 120, message = "Company name must be at most 120 characters.")
    private String companyName;

    @Size(max = 100, message = "Contact person must be at most 100 characters.")
    private String contactPerson;

    @Email(message = "Enter a valid email address.")
    @Size(max = 150, message = "Email must be at most 150 characters.")
    private String email;

    @Pattern(regexp = "^$|^\\+?[0-9][0-9 -]{6,18}[0-9]$", message = "Enter a valid phone number (7-20 digits, optional leading +).")
    private String phone;
    @Size(max = 255, message = "Address must be at most 255 characters.")
    private String address;
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
