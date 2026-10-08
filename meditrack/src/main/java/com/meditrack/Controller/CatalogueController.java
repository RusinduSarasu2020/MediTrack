package com.meditrack.controller;

import com.meditrack.model.Category;
import com.meditrack.model.Medicine;
import com.meditrack.service.CategoryService;
import com.meditrack.service.InventoryService;
import com.meditrack.service.ReviewService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Controller
@RequestMapping("/catalog")
public class CatalogueController {

    private final InventoryService inventoryService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;

    public CatalogueController(InventoryService inventoryService, CategoryService categoryService, ReviewService reviewService) {
        this.inventoryService = inventoryService;
        this.categoryService = categoryService;
        this.reviewService = reviewService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Boolean rx,
                       Model model) {
        List<Medicine> medicines = inventoryService.searchMedicines(q);

        if (categoryId != null) {
            medicines = medicines.stream()
                    .filter(m -> m.getCategory().getId().equals(categoryId))
                    .toList();
        }
        if (rx != null) {
            medicines = medicines.stream()
                    .filter(m -> m.isPrescriptionRequired() == rx)
                    .toList();
        }

        model.addAttribute("medicines", medicines);
        model.addAttribute("categories", categoryService.findActive());
        model.addAttribute("inventoryService", inventoryService);
        model.addAttribute("searchQuery", q);
        model.addAttribute("selectedCategory", categoryId);
        model.addAttribute("selectedRx", rx);

        return "catalog/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Medicine medicine = inventoryService.findMedicineById(id);
        model.addAttribute("medicine", medicine);
        model.addAttribute("effectivePrice", inventoryService.getEffectiveSellingPrice(id));
        model.addAttribute("saleableStock", inventoryService.getSaleableStockCount(id));
        model.addAttribute("reviews", reviewService.getVisibleReviewsForMedicine(id));
        return "catalog/detail";
    }
}
