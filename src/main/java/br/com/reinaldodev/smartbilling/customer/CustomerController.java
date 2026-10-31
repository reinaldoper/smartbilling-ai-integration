package br.com.reinaldodev.smartbilling.customer;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    private final CustomerRepository repository;
    public CustomerController(CustomerRepository repository) { this.repository = repository; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer create(@Valid @RequestBody CreateCustomerRequest request) {
        return repository.save(new Customer(request.name(), request.document(), request.email()));
    }

    @GetMapping
    public List<Customer> list() { return repository.findAll(); }

    public record CreateCustomerRequest(
            @NotBlank String name,
            @NotBlank String document,
            @Email @NotBlank String email) {}
}
