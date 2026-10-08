package com.meditrack.Service;

import com.meditrack.Model.dto.PrescriptionReviewForm;
import com.meditrack.Model.enums.OrderStatus;
import com.meditrack.Model.enums.PrescriptionStatus;
import com.meditrack.Model.enums.Role;
import com.meditrack.Model.exception.BusinessException;
import com.meditrack.Model.exception.ResourceNotFoundException;
import com.meditrack.Model.*;
import com.meditrack.Repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Service
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final CustomerOrderRepository orderRepository;
    private final DispenseRecordRepository dispenseRecordRepository;
    private final SaleRepository saleRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final Clock clock;

    public PrescriptionService(PrescriptionRepository prescriptionRepository, CustomerOrderRepository orderRepository,
                               DispenseRecordRepository dispenseRecordRepository, SaleRepository saleRepository,
                               AuditService auditService, NotificationService notificationService, Clock clock) {
        this.prescriptionRepository = prescriptionRepository;
        this.orderRepository = orderRepository;
        this.dispenseRecordRepository = dispenseRecordRepository;
        this.saleRepository = saleRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Prescription> findPendingPrescriptions() {
        return prescriptionRepository.findByStatusOrderByCreatedAtDesc(PrescriptionStatus.PENDING);
    }

    @Transactional(readOnly = true)
    public Prescription findById(Long id) {
        return prescriptionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + id));
    }

    @Transactional
    public Prescription reviewPrescription(PrescriptionReviewForm form, User pharmacist) {
        if (pharmacist.getRole() != Role.PHARMACIST) {
            throw new BusinessException("Only pharmacists can approve or reject prescriptions.");
        }

        Prescription prescription = findById(form.getPrescriptionId());
        CustomerOrder order = prescription.getOrder();

        if (prescription.getStatus() != PrescriptionStatus.PENDING) {
            throw new BusinessException("Prescription has already been reviewed.");
        }

        prescription.setStatus(form.getStatus());
        prescription.setReviewedBy(pharmacist);
        prescription.setReviewedAt(Instant.now());
        prescription.setReviewNotes(form.getReviewNotes());
        prescription.setApprovedRevision(order.getItemRevision());

        if (form.getStatus() == PrescriptionStatus.APPROVED) {
            order.setStatus(OrderStatus.AWAITING_PAYMENT);
            notificationService.notify(order.getCustomer().getId(),
                    "Your prescription for order #" + order.getId() + " has been APPROVED. You can now complete payment.",
                    "/orders/" + order.getId());
        } else {
            order.setStatus(OrderStatus.REJECTED);
            notificationService.notify(order.getCustomer().getId(),
                    "Your prescription for order #" + order.getId() + " was REJECTED: " + form.getReviewNotes(),
                    "/orders/" + order.getId());
        }

        orderRepository.save(order);
        Prescription saved = prescriptionRepository.save(prescription);

        auditService.log(pharmacist.getId(), "REVIEW_PRESCRIPTION", "Prescription", saved.getId(),
                "Pharmacist " + pharmacist.getUsername() + " set status to " + form.getStatus() + " for order #" + order.getId());

        return saved;
    }

    @Transactional
    public DispenseRecord dispense(Long orderId, String notes, User pharmacist) {
        if (pharmacist.getRole() != Role.PHARMACIST) {
            throw new BusinessException("Only pharmacists are authorized to record dispensing.");
        }

        CustomerOrder order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (dispenseRecordRepository.existsByOrderId(orderId)) {
            throw new BusinessException("Order #" + orderId + " has already been dispensed.");
        }

        if (order.getStatus() != OrderStatus.PAID && order.getStatus() != OrderStatus.PREPARING) {
            throw new BusinessException("Order must be PAID or PREPARING before dispensing.");
        }

        Sale sale = saleRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BusinessException("No completed sale found for order #" + orderId));

        // Check if allocated batches have expired between payment and dispensing
        LocalDate today = LocalDate.now(clock);
        for (SaleItem item : sale.getItems()) {
            if (item.getBatch().isExpired(today) || item.getBatch().isQuarantined()) {
                throw new BusinessException("Allocated batch " + item.getBatch().getBatchNumber() + " has expired or been quarantined. Dispensing stopped.");
            }
        }

        DispenseRecord record = new DispenseRecord(order, pharmacist, notes);
        DispenseRecord saved = dispenseRecordRepository.save(record);

        order.setStatus(OrderStatus.READY_FOR_COLLECTION);
        orderRepository.save(order);

        // NOTE: Stock was already deducted at successful payment! Dispensing records handover and MUST NOT deduct stock again!

        if (order.getCustomer() != null) {
            notificationService.notify(order.getCustomer().getId(),
                    "Your order #" + order.getId() + " is prepared and READY FOR COLLECTION at the pharmacy.",
                    "/orders/" + order.getId());
        }

        auditService.log(pharmacist.getId(), "DISPENSE_ORDER", "DispenseRecord", saved.getId(),
                "Pharmacist " + pharmacist.getUsername() + " recorded dispensing for order #" + order.getId());

        return saved;
    }
}
