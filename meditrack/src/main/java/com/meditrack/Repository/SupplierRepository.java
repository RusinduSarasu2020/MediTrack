package com.meditrack.repository;

import com.meditrack.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    List<Supplier> findByActiveTrueOrderByCompanyNameAsc();
    Optional<Supplier> findFirstByCompanyNameIgnoreCase(String companyName);
    Optional<Supplier> findFirstByEmailIgnoreCase(String email);
}
