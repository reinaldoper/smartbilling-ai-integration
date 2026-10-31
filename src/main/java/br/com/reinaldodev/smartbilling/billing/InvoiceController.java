package br.com.reinaldodev.smartbilling.billing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final BillingService service;
    public InvoiceController(BillingService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice create(@Valid @RequestBody CreateInvoiceRequest request) {
        return service.createInvoice(request.customerId(), request.amount(), request.dueDate());
    }

    @GetMapping("/customer/{customerId}")
    public List<Invoice> byCustomer(@PathVariable Long customerId) {
        return service.findInvoices(customerId);
    }

    @GetMapping("/customer/{customerId}/open")
    public List<Invoice> openByCustomer(@PathVariable Long customerId) {
        return service.findOpenInvoices(customerId);
    }

    @GetMapping("/{invoiceId}")
    public Invoice byId(@PathVariable Long invoiceId) { return service.findById(invoiceId); }

    public record CreateInvoiceRequest(
            @NotNull Long customerId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotNull LocalDate dueDate) {}
}
