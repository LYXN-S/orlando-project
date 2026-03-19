package com.orlandoprestige.orlandoproject.orders.internal.procurement.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "system_sales_invoices")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SystemSalesInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "purchase_order_id", nullable = false, unique = true)
    private Long purchaseOrderId;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "registered_name")
    private String registeredName;

    @Column(name = "tin")
    private String tin;

    @Column(name = "business_address", columnDefinition = "TEXT")
    private String businessAddress;

    @Column(name = "terms")
    private String terms;

    @Column(name = "po_number")
    private String poNumber;

    @Column(name = "total", precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "total_sales", precision = 14, scale = 2)
    private BigDecimal totalSales;

    @Column(name = "less_vat", precision = 14, scale = 2)
    private BigDecimal lessVat;

    @Column(name = "amount_net_of_vat", precision = 14, scale = 2)
    private BigDecimal amountNetOfVat;

    @Column(name = "less_wh_tax", precision = 14, scale = 2)
    private BigDecimal lessWhTax;

    @Column(name = "total_after_wh_tax", precision = 14, scale = 2)
    private BigDecimal totalAfterWhTax;

    @Column(name = "add_vat", precision = 14, scale = 2)
    private BigDecimal addVat;

    @Column(name = "total_amount_due", precision = 14, scale = 2)
    private BigDecimal totalAmountDue;

    @Column(name = "vatable_sales", precision = 14, scale = 2)
    private BigDecimal vatableSales;

    @Column(name = "vat", precision = 14, scale = 2)
    private BigDecimal vat;

    @Column(name = "zero_rated_sales", precision = 14, scale = 2)
    private BigDecimal zeroRatedSales;

    @Column(name = "vat_ex_sales", precision = 14, scale = 2)
    private BigDecimal vatExSales;

    @Column(name = "approved_by")
    private String approvedBy;

    @Column(name = "prepared_by")
    private String preparedBy;

    @Column(name = "prepared_by_user_id")
    private Long preparedByUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "salesInvoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SystemSalesInvoiceItem> items = new ArrayList<>();
}
