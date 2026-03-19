package com.orlandoprestige.orlandoproject.orders.internal.procurement.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "system_sales_invoice_items")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemSalesInvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sales_invoice_id", nullable = false)
    private SystemSalesInvoice salesInvoice;

    @Column(name = "line_number", nullable = false)
    private Integer lineNumber;

    @Column(name = "item_label")
    private String itemLabel;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "unit_price", precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "amount", precision = 14, scale = 2)
    private BigDecimal amount;
}
