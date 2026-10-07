package com.meditrack.service;

import com.meditrack.dto.ReviewForm;
import com.meditrack.enums.OrderStatus;
import com.meditrack.exception.BusinessException;
import com.meditrack.exception.ResourceNotFoundException;
import com.meditrack.model.*;
import com.meditrack.repository.CustomerOrderRepository;
import com.meditrack.repository.MedicineRepository;
import com.meditrack.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final CustomerOrderRepository orderRepository;
    private final MedicineRepository medicineRepository;
    private final AuditService auditService;

    public ReviewService(ReviewRepository reviewRepository, CustomerOrderRepository orderRepository,
                         MedicineRepository medicineRepository, AuditService auditService) {
        this.reviewRepository = reviewRepository;
        this.orderRepository = orderRepository;
        this.medicineRepository = medicineRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Review addReview(ReviewForm form, User customer) {
        CustomerOrder order = orderRepository.findById(form.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + form.getOrderId()));

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("You can only review purchases from your own orders.");
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new BusinessException("Reviews can only be submitted for COMPLETED orders.");
        }

        boolean foundItem = false;
        for (OrderItem item : order.getItems()) {
            if (item.getMedicine().getId().equals(form.getMedicineId())) {
                foundItem = true;
                break;
            }
        }
        if (!foundItem) {
            throw new BusinessException("This medicine was not part of order #" + order.getId());
        }

        if (reviewRepository.existsByCustomerIdAndOrderIdAndMedicineId(customer.getId(), form.getOrderId(), form.getMedicineId())) {
            throw new BusinessException("You have already reviewed this medicine for this order.");
        }

        Medicine medicine = medicineRepository.findById(form.getMedicineId()).get();
        Review review = new Review(customer, order, medicine, form.getRating(), form.getComment());
        Review saved = reviewRepository.save(review);
        auditService.log(customer.getId(), "ADD_REVIEW", "Review", saved.getId(), "Added review for medicine: " + medicine.getSku());
        return saved;
    }

    @Transactional
    public Review updateReview(Long reviewId, ReviewForm form, User customer) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("You can only edit your own reviews.");
        }

        review.setRating(form.getRating());
        review.setComment(form.getComment());
        review.setUpdatedAt(Instant.now());
        Review saved = reviewRepository.save(review);
        auditService.log(customer.getId(), "UPDATE_REVIEW", "Review", saved.getId(), "Updated own review #" + saved.getId());
        return saved;
    }

    @Transactional
    public void deleteReview(Long reviewId, User customer) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getCustomer().getId().equals(customer.getId())) {
            throw new BusinessException("You can only delete your own reviews.");
        }

        reviewRepository.delete(review);
        auditService.log(customer.getId(), "DELETE_REVIEW", "Review", reviewId, "Deleted own review #" + reviewId);
    }

    @Transactional
    public void moderateReview(Long reviewId, boolean visible, Long adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        review.setVisible(visible);
        reviewRepository.save(review);
        auditService.log(adminId, "MODERATE_REVIEW", "Review", reviewId, "Moderated review visibility: " + visible);
    }

    @Transactional(readOnly = true)
    public List<Review> getVisibleReviewsForMedicine(Long medicineId) {
        return reviewRepository.findByMedicineIdAndVisibleTrueOrderByUpdatedAtDesc(medicineId);
    }

    @Transactional(readOnly = true)
    public List<Review> getCustomerReviews(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByUpdatedAtDesc(customerId);
    }
}
