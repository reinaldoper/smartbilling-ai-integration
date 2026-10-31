package br.com.reinaldodev.smartbilling.billing;

import br.com.reinaldodev.smartbilling.customer.CustomerRepository;
import br.com.reinaldodev.smartbilling.outbox.OutboxEvent;
import br.com.reinaldodev.smartbilling.outbox.OutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {
    private final InvoiceRepository invoices;
    private final CustomerRepository customers;
    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public BillingService(InvoiceRepository invoices, CustomerRepository customers,
                          OutboxRepository outbox, ObjectMapper mapper) {
        this.invoices = invoices; this.customers = customers;
        this.outbox = outbox; this.mapper = mapper;
    }

    @Transactional
    public Invoice createInvoice(Long customerId, BigDecimal amount, LocalDate dueDate) {
        var customer = customers.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + customerId));
        var invoice = invoices.save(new Invoice(customer, amount, dueDate));
        try {
            var payload = mapper.writeValueAsString(Map.of(
                    "eventId", java.util.UUID.randomUUID().toString(),
                    "invoiceId", invoice.getId(),
                    "customerId", customerId,
                    "amount", amount,
                    "dueDate", dueDate.toString(),
                    "status", invoice.getStatus().name()
            ));
            outbox.save(new OutboxEvent("INVOICE", invoice.getId().toString(),
                    "INVOICE_CREATED", payload));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Não foi possível serializar evento", e);
        }
        return invoice;
    }

    @Transactional(readOnly = true)
    public List<Invoice> findInvoices(Long customerId) {
        return invoices.findByCustomerIdOrderByDueDateAsc(customerId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> findOpenInvoices(Long customerId) {
        return invoices.findByCustomerIdAndStatusOrderByDueDateAsc(customerId, InvoiceStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public Invoice findById(Long invoiceId) {
        return invoices.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Cobrança não encontrada: " + invoiceId));
    }
}
