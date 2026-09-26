package com.meditrack.meditrack.repository;

import com.meditrack.meditrack.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByStatus(Supplier.SupplierStatus status);

    List<Supplier> findByCompanyNameContainingIgnoreCase(String companyName);
}
