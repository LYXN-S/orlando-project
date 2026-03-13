package com.orlandoprestige.orlandoproject.orders.internal.procurement.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_purchase_order_extraction_runs")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemPurchaseOrderExtractionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_id", nullable = false)
    private Long purchaseOrderId;

    @Column(name = "attachment_id", nullable = false)
    private Long attachmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SystemPoExtractionStatus status;

    @Column(name = "model_name")
    private String modelName;

    @Column(name = "confidence")
    private Double confidence;

    @Column(name = "warnings", columnDefinition = "TEXT")
    private String warnings;

    @Column(name = "extracted_json", columnDefinition = "TEXT")
    private String extractedJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
