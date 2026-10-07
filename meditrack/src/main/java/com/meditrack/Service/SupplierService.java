package com.meditrack.meditrack.service;

import com.meditrack.meditrack.model.Supplier;
import com.meditrack.meditrack.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<Supplier> getAllSuppliers() {
        return supplierRepository.findAll();
    }

    public List<Supplier> searchByCompanyName(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllSuppliers();
        }
        return supplierRepository.findByCompanyNameContainingIgnoreCase(keyword);
    }

    public Supplier getSupplierById(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Supplier not found with id: " + id));
    }

    public Supplier saveSupplier(Supplier supplier) {
        return supplierRepository.save(supplier);
    }

    public void deleteSupplier(Long id) {
        supplierRepository.deleteById(id);
    }

    public List<Supplier> getActiveSuppliers() {
        return supplierRepository.findByStatus(Supplier.SupplierStatus.ACTIVE);
    }
}
