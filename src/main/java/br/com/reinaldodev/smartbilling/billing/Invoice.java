package br.com.reinaldodev.smartbilling.billing;

import br.com.reinaldodev.smartbilling.customer.Customer;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "invoices")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private Customer customer;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false)
    private LocalDate dueDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.OPEN;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    protected Invoice() {}
    public Invoice(Customer customer, BigDecimal amount, LocalDate dueDate) {
        this.customer = customer; this.amount = amount; this.dueDate = dueDate;
    }
    public Long getId() { return id; }
    public Customer getCustomer() { return customer; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public InvoiceStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public void markPaid() { this.status = InvoiceStatus.PAID; }
}
