package com.meditrack.service;

import com.meditrack.dto.CartItemDto;
import com.meditrack.model.Medicine;
import com.meditrack.repository.MedicineRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@SessionScope
public class CartService {

    private final Map<Long, Integer> items = new LinkedHashMap<>();
    private final MedicineRepository medicineRepository;
    private final InventoryService inventoryService;

    public CartService(MedicineRepository medicineRepository, InventoryService inventoryService) {
        this.medicineRepository = medicineRepository;
        this.inventoryService = inventoryService;
    }

    public void addItem(Long medicineId, int quantity) {
        if (quantity <= 0) return;
        items.put(medicineId, items.getOrDefault(medicineId, 0) + quantity);
    }

    public void updateItem(Long medicineId, int quantity) {
        if (quantity <= 0) {
            items.remove(medicineId);
        } else {
            items.put(medicineId, quantity);
        }
    }

    public void removeItem(Long medicineId) {
        items.remove(medicineId);
    }

    public void clear() {
        items.clear();
    }

    public List<CartItemDto> getItems() {
        List<CartItemDto> list = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            medicineRepository.findById(entry.getKey()).ifPresent(med -> {
                if (med.isActive()) {
                    BigDecimal price = inventoryService.getEffectiveSellingPrice(med.getId());
                    list.add(new CartItemDto(med.getId(), med.getSku(), med.getBrandName() + " (" + med.getGenericName() + ")",
                            price, entry.getValue(), med.isPrescriptionRequired()));
                }
            });
        }
        return list;
    }

    public BigDecimal getSubtotal() {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItemDto item : getItems()) {
            subtotal = subtotal.add(item.getSubtotal());
        }
        return subtotal;
    }

    public boolean hasPrescriptionItems() {
        for (CartItemDto item : getItems()) {
            if (item.isPrescriptionRequired()) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }
}
